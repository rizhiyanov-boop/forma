package com.forma.app.data.repository

import com.forma.app.data.local.dao.ReviewDao
import com.forma.app.data.local.entity.ReviewEntity
import com.forma.app.data.remote.EnrichmentInput
import com.forma.app.data.remote.OpenAiService
import com.forma.app.data.remote.dto.RecommendationDto
import com.forma.app.domain.coaching.EngineRecommendation
import com.forma.app.domain.coaching.HistoryPointBuilder
import com.forma.app.domain.coaching.ProgressionAction
import com.forma.app.domain.coaching.ProgressionEngine
import com.forma.app.domain.coaching.SanityResult
import com.forma.app.domain.coaching.SanityValidator
import com.forma.app.domain.model.WorkoutSession
import com.forma.app.domain.repository.ProgramRepository
import com.forma.app.domain.repository.SessionRepository
import com.forma.app.domain.repository.UserProfileRepository
import com.forma.app.domain.review.Recommendation
import com.forma.app.domain.review.RecommendationType
import com.forma.app.domain.review.ReviewRepository
import com.forma.app.domain.review.ReviewTrigger
import com.forma.app.domain.review.Verdict
import com.forma.app.domain.review.WorkoutReview
import com.forma.app.domain.wellness.WellnessRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val reviewDao: ReviewDao,
    private val sessionRepo: SessionRepository,
    private val programRepo: ProgramRepository,
    private val profileRepo: UserProfileRepository,
    private val wellnessRepo: WellnessRepository,
    private val openAi: OpenAiService
) : ReviewRepository {

    // Движок и валидатор — без зависимостей, инициализируем сами.
    // Если в будущем понадобится конфигурация — переведём через DI.
    private val engine = ProgressionEngine()
    private val validator = SanityValidator()
    private val wellnessContextBuilder = WellnessContextBuilder(wellnessRepo)

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    override suspend fun reviewAfterWorkout(sessionId: String): String {
        // Если разбор для этой сессии уже есть — возвращаем существующий
        val existing = reviewDao.getBySessionId(sessionId)
        if (existing != null) return existing.id

        val session = sessionRepo.getSession(sessionId)
            ?: error("Сессия не найдена: $sessionId")
        val program = programRepo.getActiveProgram()
            ?: error("Нет активной программы")
        val workout = program.workouts.firstOrNull { it.id == session.workoutId }
            ?: error("Тренировка не найдена в программе")

        val recent = sessionRepo.recentSessionsForWorkout(
            workoutId = workout.id,
            excludeId = session.id,
            limit = 4
        )

        android.util.Log.d("Forma.Review",
            "Generating review for session=$sessionId, recentCount=${recent.size}")

        // ─── 1. Загружаем профиль пользователя (нужен для движка и AI) ─────
        val profile = profileRepo.observeProfile().firstOrNull()
            ?: error("Профиль пользователя не найден")

        // ─── 2. Прогоняем ProgressionEngine для каждого упражнения тренировки ─
        // Для каждого упражнения собираем историю (текущая сессия + recent)
        // и получаем алгоритмическое решение со всеми числами.
        val engineDecisions = workout.exercises.mapNotNull { exercise ->
            val history = buildHistoryForExercise(exercise.id, session, recent)
            if (history.isEmpty()) return@mapNotNull null
            engine.analyze(
                exercise = exercise,
                history = history,
                goal = profile.goal,
                level = profile.level
            )
        }

        // ─── 3. Sanity-валидация каждой рекомендации ─────────────────────
        val sanitized = engineDecisions.map { decision ->
            val ex = workout.exercises.first { it.id == decision.exerciseId }
            val daysSinceDeload = daysSinceLastDeload(decision.exerciseId)
            validator.validate(
                rec = decision,
                currentWeight = ex.startingWeightKg ?: decision.signals.workingWeightLast,
                daysSinceLastDeload = daysSinceDeload
            )
        }

        // ─── 4. Готовим вход для AI: только actionable рекомендации ──────
        // KEEP/INSUFFICIENT_DATA пропускаем — AI их и так упомянет в summary
        val enrichmentInputs = sanitized
            .filter { it.rec.action != ProgressionAction.INSUFFICIENT_DATA }
            .map { it.toEnrichmentInput() }

        android.util.Log.d("Forma.Review",
            "Engine produced ${engineDecisions.size} decisions, " +
                "sanitized to ${sanitized.count { it.isDisputed }} disputed, " +
                "${enrichmentInputs.size} sent to AI for enrichment")

        // ─── 5. AI обогащает: пишет summary, выбирает что показать, переписывает обоснования
        val wellnessContext = wellnessContextBuilder.build(sessionId)

        val dto = openAi.enrichEngineReview(
            session = session,
            workout = workout,
            engineRecommendations = enrichmentInputs,
            userSex = profile.sex,
            wellnessContext = wellnessContext
        )

        // ─── 6. Конвертируем DTO → доменные Recommendation ─────────────────
        // AI вернёт рекомендации с тем же exerciseId что мы передали и теми же числами
        // (мы это требуем в промпте). Если AI всё-таки изменил числа — оставляем
        // всё равно его версию, доверяя что это его решение по конкретному кейсу.
        val recommendations = dto.recommendations.map { it.toDomain() }
        val verdict = runCatching { Verdict.valueOf(dto.verdict) }.getOrDefault(Verdict.SOLID)

        val entity = ReviewEntity(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            sessionId = sessionId,
            triggerType = ReviewTrigger.AFTER_WORKOUT.name,
            verdict = verdict.name,
            summary = dto.summary,
            recommendationsJson = json.encodeToString(
                ListSerializer(Recommendation.serializer()),
                recommendations
            ),
            isApplied = false,
            isDismissed = false,
            appliedRecommendationIdsJson = "[]"
        )
        reviewDao.insert(entity)

        android.util.Log.d("Forma.Review",
            "Review created: id=${entity.id}, verdict=${verdict.name}, recs=${recommendations.size}")

        return entity.id
    }

    /**
     * Собирает историю выступлений упражнения из текущей сессии + recent.
     * Возвращает точки от старых к новым.
     */
    private fun buildHistoryForExercise(
        exerciseId: String,
        currentSession: WorkoutSession,
        recent: List<WorkoutSession>
    ): List<com.forma.app.domain.coaching.ExerciseHistoryPoint> {
        val allSessions = (recent + currentSession).sortedBy { it.startedAt }
        return allSessions.mapNotNull { sess ->
            val log = sess.exerciseLogs.firstOrNull { it.exerciseId == exerciseId }
                ?: return@mapNotNull null
            HistoryPointBuilder.fromLog(sess.id, sess.startedAt, log)
        }
    }

    /**
     * Сколько дней назад был последний DELOAD по этому упражнению.
     * Возвращает null если DELOAD-ов не было.
     */
    private suspend fun daysSinceLastDeload(exerciseId: String): Int? {
        // Берём все разборы за последние 60 дней, ищем DELOAD рекомендации
        val cutoff = System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000
        val recentReviews = reviewDao.all().filter { it.createdAt >= cutoff }
        for (review in recentReviews.sortedByDescending { it.createdAt }) {
            val recs = runCatching {
                json.decodeFromString(
                    ListSerializer(Recommendation.serializer()),
                    review.recommendationsJson
                )
            }.getOrDefault(emptyList())
            val applied = runCatching {
                json.decodeFromString(
                    ListSerializer(String.serializer()),
                    review.appliedRecommendationIdsJson
                )
            }.getOrDefault(emptyList())
            // Ищем применённую DELOAD-рекомендацию для этого упражнения
            val hadDeload = recs.any { rec ->
                rec.id in applied &&
                    rec.type == RecommendationType.DELOAD &&
                    exerciseId in rec.applicableExerciseIds
            }
            if (hadDeload) {
                val ageDays = (System.currentTimeMillis() - review.createdAt) /
                    (24L * 60 * 60 * 1000)
                return ageDays.toInt()
            }
        }
        return null
    }

    /**
     * Конвертация SanityResult → EnrichmentInput для передачи в AI.
     */
    private fun SanityResult.toEnrichmentInput(): EnrichmentInput = EnrichmentInput(
        exerciseId = rec.exerciseId,
        exerciseName = rec.exerciseName,
        action = rec.action.name,
        confidence = rec.confidence.name,
        newWeightKg = rec.newWeightKg,
        newRepsMin = rec.newRepsMin,
        newRepsMax = rec.newRepsMax,
        engineRationale = rec.rationale,
        isDisputed = isDisputed,
        disputeReason = reason,
        sessionsAnalyzed = rec.signals.sessionsAnalyzed,
        avgRirLast = rec.signals.avgRirLast,
        avgRepsLast = rec.signals.avgRepsLast,
        workingWeightLast = rec.signals.workingWeightLast,
        targetRir = rec.signals.targetRir
    )

    override fun observeReview(reviewId: String): Flow<WorkoutReview?> =
        reviewDao.observe(reviewId).map { it?.toDomain() }

    override fun observeRecentReviews(limit: Int): Flow<List<WorkoutReview>> =
        reviewDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun applyRecommendation(reviewId: String, recommendationId: String) {
        val entity = reviewDao.get(reviewId) ?: return
        val review = entity.toDomain()
        val rec = review.recommendations.firstOrNull { it.id == recommendationId } ?: return

        // Применяем по типу
        when (rec.type) {
            RecommendationType.INCREASE_WEIGHT,
            RecommendationType.DECREASE_WEIGHT -> {
                rec.applicableExerciseIds.forEach { exId ->
                    programRepo.updateExerciseFields(
                        exerciseId = exId,
                        newWeightKg = rec.newWeightKg
                    )
                }
            }
            RecommendationType.CHANGE_REPS -> {
                rec.applicableExerciseIds.forEach { exId ->
                    programRepo.updateExerciseFields(
                        exerciseId = exId,
                        newRepsMin = rec.newRepsMin,
                        newRepsMax = rec.newRepsMax
                    )
                }
            }
            RecommendationType.REST_LONGER,
            RecommendationType.REST_SHORTER -> {
                rec.applicableExerciseIds.forEach { exId ->
                    programRepo.updateExerciseFields(
                        exerciseId = exId,
                        newRestSeconds = rec.newRestSeconds
                    )
                }
            }
            // Эти типы не меняют программу — просто помечаем как принятые
            RecommendationType.DELOAD,
            RecommendationType.KEEP,
            RecommendationType.TECHNIQUE,
            RecommendationType.INFO -> Unit
        }

        // Обновляем applied list
        val applied = json.decodeFromString(
            ListSerializer(String.serializer()),
            entity.appliedRecommendationIdsJson
        ).toMutableList()
        if (recommendationId !in applied) applied += recommendationId

        val allAppliedOrDismissed = review.recommendations.all {
            it.id in applied
        }

        reviewDao.update(entity.copy(
            appliedRecommendationIdsJson = json.encodeToString(
                ListSerializer(String.serializer()),
                applied
            ),
            isApplied = allAppliedOrDismissed,
            isDirty = true
        ))
    }

    override suspend fun dismissReview(reviewId: String) {
        val entity = reviewDao.get(reviewId) ?: return
        reviewDao.update(entity.copy(isDismissed = true, isDirty = true))
    }

    // ─── Маппинг ─────────────────────────────────────────────────────

    private fun ReviewEntity.toDomain(): WorkoutReview {
        val recs = runCatching {
            json.decodeFromString(
                ListSerializer(Recommendation.serializer()),
                recommendationsJson
            )
        }.getOrDefault(emptyList())
        val applied = runCatching {
            json.decodeFromString(
                ListSerializer(String.serializer()),
                appliedRecommendationIdsJson
            )
        }.getOrDefault(emptyList())

        return WorkoutReview(
            id = id,
            createdAt = createdAt,
            sessionId = sessionId,
            triggerType = runCatching { ReviewTrigger.valueOf(triggerType) }
                .getOrDefault(ReviewTrigger.AFTER_WORKOUT),
            verdict = runCatching { Verdict.valueOf(verdict) }.getOrDefault(Verdict.SOLID),
            summary = summary,
            recommendations = recs,
            isApplied = isApplied,
            isDismissed = isDismissed,
            appliedRecommendationIds = applied
        )
    }

    private fun RecommendationDto.toDomain(): Recommendation = Recommendation(
        id = UUID.randomUUID().toString(),
        type = runCatching { RecommendationType.valueOf(type) }
            .getOrDefault(RecommendationType.INFO),
        title = title,
        rationale = rationale,
        applicableExerciseIds = applicableExerciseIds,
        newWeightKg = newWeightKg,
        newRepsMin = newRepsMin,
        newRepsMax = newRepsMax,
        newRestSeconds = newRestSeconds
    )
}
