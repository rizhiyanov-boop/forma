package com.forma.app.data.backup

import com.forma.app.data.local.dao.ExerciseOverrideDao
import com.forma.app.data.local.dao.ReviewDao
import com.forma.app.data.local.dao.SessionDao
import com.forma.app.data.local.entity.ExerciseOverrideEntity
import com.forma.app.data.local.entity.ReviewEntity
import com.forma.app.data.mapper.Mappers
import com.forma.app.domain.backup.BackupRepository
import com.forma.app.domain.backup.BackupSnapshot
import com.forma.app.domain.backup.OverrideSnapshot
import com.forma.app.domain.backup.RestoreSummary
import com.forma.app.domain.model.Equipment
import com.forma.app.domain.model.MuscleGroup
import com.forma.app.domain.repository.ProgramRepository
import com.forma.app.domain.repository.SessionRepository
import com.forma.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val profileRepo: UserProfileRepository,
    private val programRepo: ProgramRepository,
    private val sessionRepo: SessionRepository,
    private val sessionDao: SessionDao,
    private val reviewDao: ReviewDao,
    private val overrideDao: ExerciseOverrideDao
) : BackupRepository {

    private val json = Json {
        ignoreUnknownKeys = true        // чтобы старые снапшоты с лишними полями не падали
        encodeDefaults = true
        prettyPrint = true              // удобнее читать глазами и diffить
    }

    override suspend fun createSnapshot(): BackupSnapshot {
        val profile = profileRepo.observeProfile().firstOrNull()
        val program = programRepo.getActiveProgram()

        // Все сессии из БД (включая активные/незавершённые)
        val sessions = sessionDao.allSessions()
            .map { with(Mappers) { it.toDomain() } }

        // Все разборы
        val reviews = reviewDao.all().map { it.toDomainReview() }

        // Все overrides
        val overrides = overrideDao.all().map { it.toSnapshot() }

        return BackupSnapshot(
            createdAt = System.currentTimeMillis(),
            appVersion = "0.1.0-alpha",
            profile = profile,
            program = program,
            sessions = sessions,
            reviews = reviews,
            overrides = overrides
        )
    }

    override suspend fun restoreFromSnapshot(snapshot: BackupSnapshot): RestoreSummary {
        // Чистим всё перед записью — иначе будут конфликты по PK
        sessionDao.deleteAllSessions()
        reviewDao.deleteAll()
        overrideDao.deleteAll()

        // Профиль
        snapshot.profile?.let { profileRepo.saveProfile(it) }

        // Программа — создаст workouts/exercises
        snapshot.program?.let { programRepo.saveAsActive(it) }

        // Сессии — пишем через repository чтобы не возиться с маппингом обратно
        snapshot.sessions.forEach { session ->
            sessionRepo.saveSession(session)
        }

        // Разборы — пишем напрямую через DAO
        snapshot.reviews.forEach { review ->
            reviewDao.insert(review.toEntity())
        }

        // Overrides
        snapshot.overrides.forEach { ov ->
            overrideDao.upsert(ov.toEntity())
        }

        return RestoreSummary(
            sessionsImported = snapshot.sessions.size,
            reviewsImported = snapshot.reviews.size,
            overridesImported = snapshot.overrides.size,
            programLoaded = snapshot.program != null,
            profileLoaded = snapshot.profile != null
        )
    }

    override fun encodeToJson(snapshot: BackupSnapshot): String =
        json.encodeToString(BackupSnapshot.serializer(), snapshot)

    override fun decodeFromJson(jsonStr: String): BackupSnapshot =
        json.decodeFromString(BackupSnapshot.serializer(), jsonStr)

    // ─── Маппинг отзывов ────────────────────────────────────────────

    private fun ReviewEntity.toDomainReview(): com.forma.app.domain.review.WorkoutReview {
        val recsList = runCatching {
            json.decodeFromString(
                ListSerializer(
                    com.forma.app.domain.review.Recommendation.serializer()
                ),
                recommendationsJson
            )
        }.getOrDefault(emptyList())
        val applied = runCatching {
            json.decodeFromString(
                ListSerializer(
                    String.serializer()
                ),
                appliedRecommendationIdsJson
            )
        }.getOrDefault(emptyList())
        return com.forma.app.domain.review.WorkoutReview(
            id = id,
            createdAt = createdAt,
            sessionId = sessionId,
            triggerType = runCatching {
                com.forma.app.domain.review.ReviewTrigger.valueOf(triggerType)
            }.getOrDefault(com.forma.app.domain.review.ReviewTrigger.AFTER_WORKOUT),
            verdict = runCatching {
                com.forma.app.domain.review.Verdict.valueOf(verdict)
            }.getOrDefault(com.forma.app.domain.review.Verdict.SOLID),
            summary = summary,
            recommendations = recsList,
            isApplied = isApplied,
            isDismissed = isDismissed,
            appliedRecommendationIds = applied
        )
    }

    private fun com.forma.app.domain.review.WorkoutReview.toEntity(): ReviewEntity = ReviewEntity(
        id = id,
        createdAt = createdAt,
        sessionId = sessionId,
        triggerType = triggerType.name,
        verdict = verdict.name,
        summary = summary,
        recommendationsJson = json.encodeToString(
            ListSerializer(
                com.forma.app.domain.review.Recommendation.serializer()
            ),
            recommendations
        ),
        isApplied = isApplied,
        isDismissed = isDismissed,
        appliedRecommendationIdsJson = json.encodeToString(
            ListSerializer(
                String.serializer()
            ),
            appliedRecommendationIds
        )
    )

    // ─── Маппинг overrides ──────────────────────────────────────────

    private fun ExerciseOverrideEntity.toSnapshot(): OverrideSnapshot = OverrideSnapshot(
        id = id,
        workoutId = workoutId,
        exerciseIdOrigin = exerciseIdOrigin,
        name = name,
        description = description,
        primaryMuscle = primaryMuscle.name,
        secondaryMuscles = secondaryMuscles.map { it.name },
        equipment = equipment.map { it.name },
        targetSets = targetSets,
        targetRepsMin = targetRepsMin,
        targetRepsMax = targetRepsMax,
        restSeconds = restSeconds,
        usesWeight = usesWeight,
        notes = notes,
        startingWeightKg = startingWeightKg,
        targetSetsDetailedJson = targetSetsDetailedJson,
        createdAt = createdAt
    )

    private fun OverrideSnapshot.toEntity(): ExerciseOverrideEntity = ExerciseOverrideEntity(
        id = id,
        workoutId = workoutId,
        exerciseIdOrigin = exerciseIdOrigin,
        name = name,
        description = description,
        primaryMuscle = runCatching { MuscleGroup.valueOf(primaryMuscle) }
            .getOrDefault(MuscleGroup.FULL_BODY),
        secondaryMuscles = secondaryMuscles.mapNotNull {
            runCatching { MuscleGroup.valueOf(it) }.getOrNull()
        },
        equipment = equipment.mapNotNull {
            runCatching { Equipment.valueOf(it) }.getOrNull()
        },
        targetSets = targetSets,
        targetRepsMin = targetRepsMin,
        targetRepsMax = targetRepsMax,
        restSeconds = restSeconds,
        usesWeight = usesWeight,
        notes = notes,
        startingWeightKg = startingWeightKg,
        targetSetsDetailedJson = targetSetsDetailedJson,
        createdAt = createdAt
    )
}
