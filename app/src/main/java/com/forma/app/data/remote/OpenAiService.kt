package com.forma.app.data.remote

import com.forma.app.data.remote.api.OpenAiApi
import com.forma.app.data.remote.dto.ChatMessage
import com.forma.app.data.remote.dto.ChatRequest
import com.forma.app.data.remote.dto.JsonSchemaWrapper
import com.forma.app.data.remote.dto.ProgramPlanDto
import com.forma.app.data.remote.dto.ReplacementSuggestionsDto
import com.forma.app.data.remote.dto.ResponseFormat
import com.forma.app.domain.model.DayOfWeek
import com.forma.app.domain.model.UserProfile
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Какую модель используем для какого сценария.
 * Меняй здесь чтобы переключить модель в одном месте — не разбросано по коду.
 *
 * История:
 *  - Изначально везде стоял gpt-4o-mini — работал стабильно.
 *  - Пробовал gpt-5.4-mini для генерации/разбора, но AI начал возвращать неполные ответы
 *    (1 тренировка из 3). Возможно в Chat Completions API слаг другой.
 *  - Откатил на gpt-4o-mini для генерации/замены.
 *  - Для разбора оставил o4-mini — reasoning модель, в проверенном API.
 *  - Добавлен флагман для эскалации генерации программы: если mini после двух попыток
 *    не даёт нужное число дней — переходим на gpt-4o (более старший, проверенный).
 */
private object Models {
    const val GENERATE_PROGRAM = "gpt-4o-mini"
    /** Эскалация если mini не справился с количеством дней. */
    const val GENERATE_PROGRAM_FLAGSHIP = "gpt-4o"
    const val SUGGEST_REPLACEMENT = "gpt-4o-mini"
    const val QUICK_REVIEW = "o4-mini"
}

@Singleton
class OpenAiService @Inject constructor(
    private val api: OpenAiApi,
    private val json: Json
) {

    /**
     * Генерирует недельную программу тренировок под профиль пользователя.
     * Использует Structured Outputs — ответ гарантированно валиден по схеме.
     */
    suspend fun generateProgram(profile: UserProfile): ProgramPlanDto {
        val systemPrompt = buildSystemPrompt()
        val userPrompt = buildUserPrompt(profile)

        // Попытка 1 — основная модель (mini, дёшево)
        val firstAttempt = tryGenerate(
            model = Models.GENERATE_PROGRAM,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            attempt = 1
        )
        if (firstAttempt.workouts.size >= profile.daysPerWeek) {
            return firstAttempt
        }

        // Попытка 2 — с явной обратной связью + основная модель
        // (часто mini выправляется когда явно сказать «ты вернул X, нужно N»)
        android.util.Log.w("Forma.OpenAi",
            "Attempt 1 returned ${firstAttempt.workouts.size}/${profile.daysPerWeek}, retrying with feedback")

        val feedbackPrompt = buildString {
            appendLine(userPrompt)
            appendLine()
            appendLine("════════════════════════════════════════════")
            appendLine("ВАЖНО: твой предыдущий ответ содержал только ${firstAttempt.workouts.size} тренировку.")
            appendLine("Нужно РОВНО ${profile.daysPerWeek}. Перечитай чек-лист и сформируй полный массив.")
            appendLine("════════════════════════════════════════════")
        }

        val secondAttempt = tryGenerate(
            model = Models.GENERATE_PROGRAM,
            systemPrompt = systemPrompt,
            userPrompt = feedbackPrompt,
            attempt = 2
        )
        if (secondAttempt.workouts.size >= profile.daysPerWeek) {
            android.util.Log.d("Forma.OpenAi", "Attempt 2 succeeded with mini")
            return secondAttempt
        }

        // Попытка 3 — эскалация на флагман
        android.util.Log.w("Forma.OpenAi",
            "Attempt 2 still bad (${secondAttempt.workouts.size}), escalating to flagship")

        val flagshipAttempt = tryGenerate(
            model = Models.GENERATE_PROGRAM_FLAGSHIP,
            systemPrompt = systemPrompt,
            userPrompt = feedbackPrompt,
            attempt = 3
        )
        if (flagshipAttempt.workouts.size >= profile.daysPerWeek) {
            android.util.Log.d("Forma.OpenAi", "Attempt 3 succeeded with flagship")
            return flagshipAttempt
        }

        // Совсем не получилось — отдаём ошибку с диагностикой
        throw IllegalStateException(
            "AI не смог сгенерировать программу на ${profile.daysPerWeek} дней " +
                "после 3 попыток (последняя дала ${flagshipAttempt.workouts.size}). Попробуй ещё раз."
        )
    }

    /**
     * Один вызов API без валидации количества дней.
     */
    private suspend fun tryGenerate(
        model: String,
        systemPrompt: String,
        userPrompt: String,
        attempt: Int
    ): ProgramPlanDto {
        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            ),
            temperature = 0.4,
            responseFormat = ResponseFormat(
                type = "json_schema",
                jsonSchema = JsonSchemaWrapper(
                    name = "weekly_program",
                    strict = true,
                    schema = ProgramSchemas.programPlanSchema()
                )
            )
        )

        android.util.Log.d("Forma.OpenAi", "Attempt $attempt: model=$model")
        val response = api.chat(request)
        val content = response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Пустой ответ от OpenAI: choices пустые")

        android.util.Log.d("Forma.OpenAi", "Attempt $attempt got response, length=${content.length}")

        return try {
            val plan = json.decodeFromString(ProgramPlanDto.serializer(), content)
            android.util.Log.d("Forma.OpenAi",
                "Attempt $attempt parsed: workouts=${plan.workouts.size}, " +
                "days=[${plan.workouts.joinToString { it.dayOfWeek }}]")
            plan
        } catch (t: Throwable) {
            android.util.Log.e("Forma.OpenAi", "Attempt $attempt failed to parse: $content", t)
            throw IllegalStateException("Не удалось распарсить ответ AI: ${t.message}", t)
        }
    }

    /**
     * Подбирает 2-3 варианта замены конкретного упражнения.
     * @param profile профиль пользователя (для учёта оборудования и уровня)
     * @param workoutFocus фокус тренировки (например "Грудь + Трицепс")
     * @param toReplace упражнение, которое нужно заменить
     * @param reason необязательная причина (боль, нет инвентаря, не подходит)
     */
    suspend fun suggestReplacement(
        profile: UserProfile,
        workoutFocus: String,
        toReplace: com.forma.app.domain.model.Exercise,
        reason: String?
    ): ReplacementSuggestionsDto {
        val systemPrompt = """
            Ты — опытный тренер. Подбираешь альтернативу одному упражнению в составе тренировки.
            
            Правила:
            - Возвращай ровно 2-3 варианта в массиве suggestions.
            - Альтернативы должны нагружать ту же первичную группу мышц.
            - Не предлагай упражнения, требующие недоступного пользователю оборудования.
            - Учитывай уровень и причину замены: если есть боль — варианты должны быть щадящими.
            - Сохраняй похожий объём (количество подходов и диапазон повторов).
            - Названия и описания на русском языке.
            - Отвечай ТОЛЬКО в JSON согласно предоставленной схеме.
        """.trimIndent()

        val equipment = profile.equipment.joinToString(", ") { it.displayName }
        val reasonStr = reason?.takeIf { it.isNotBlank() } ?: "не указана"
        val userPrompt = """
            Замени упражнение в рамках тренировки.
            
            Контекст тренировки: $workoutFocus
            Уровень пользователя: ${profile.level.displayName}
            Цель: ${profile.goal.displayName}
            Пол: ${profile.sex.displayName}
            Доступное оборудование: $equipment
            
            Заменяемое упражнение:
            - Название: ${toReplace.name}
            - Первичная группа: ${toReplace.primaryMuscle.displayName}
            - Подходов: ${toReplace.targetSets}, повторов: ${toReplace.targetRepsMin}-${toReplace.targetRepsMax}
            - С весом: ${if (toReplace.usesWeight) "да" else "нет"}
            
            Причина замены: $reasonStr
        """.trimIndent()

        val request = ChatRequest(
            model = Models.SUGGEST_REPLACEMENT,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            ),
            temperature = 0.6,
            responseFormat = ResponseFormat(
                type = "json_schema",
                jsonSchema = JsonSchemaWrapper(
                    name = "exercise_replacements",
                    strict = true,
                    schema = ProgramSchemas.replacementSuggestionsSchema()
                )
            )
        )

        android.util.Log.d("Forma.OpenAi", "Requesting replacements for: ${toReplace.name}, reason: $reasonStr")
        val response = api.chat(request)
        val content = response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Пустой ответ от OpenAI")

        return try {
            json.decodeFromString(ReplacementSuggestionsDto.serializer(), content)
        } catch (t: Throwable) {
            android.util.Log.e("Forma.OpenAi", "Failed to parse replacement response: $content", t)
            throw IllegalStateException("Не удалось распарсить ответ AI: ${t.message}", t)
        }
    }

    /**
     * Быстрый разбор после тренировки. Принимает завершённую сессию + контекст:
     * последние 4 сессии того же workout (для сравнения) и активную программу.
     */
    suspend fun quickReviewAfterWorkout(
        session: com.forma.app.domain.model.WorkoutSession,
        workout: com.forma.app.domain.model.Workout,
        recentSessions: List<com.forma.app.domain.model.WorkoutSession>
    ): com.forma.app.data.remote.dto.QuickReviewDto {
        val systemPrompt = """
            Ты — опытный тренер. Делаешь короткий разбор последней тренировки пользователя.
            
            Правила:
            - Анализируй фактические подходы (вес, повторы, RIR) — что выполнено и в каком качестве.
            - Сравнивай с последними тренировками того же дня: есть ли прогресс, застой, регресс?
            - Формируй 1-4 КОНКРЕТНЫЕ рекомендации с привязкой к exerciseId из переданных упражнений.
            - Рекомендации должны быть применимы автоматически: для INCREASE_WEIGHT/DECREASE_WEIGHT укажи новый вес в newWeightKg, для CHANGE_REPS — диапазон, для REST_LONGER/SHORTER — секунды отдыха.
            - Если объективно нечего менять — одна рекомендация типа KEEP с пустым applicableExerciseIds.
            - Тон: прямой, второе лицо ("ты"), без воды. Используй RIR и веса в обосновании.
            - Все тексты на русском.
            
            Вердикты:
            - PROGRESS: рост весов или повторов vs прошлая такая же тренировка
            - REGRESS: падение весов/повторов vs прошлой
            - STAGNATION: 3+ тренировки подряд одни и те же показатели без изменения
            - EARLY: меньше 2 тренировок этого дня — нет данных для сравнения
            - SOLID: всё стабильно в рабочем диапазоне без явного изменения
            
            Отвечай ТОЛЬКО JSON по схеме.
        """.trimIndent()

        val userPrompt = buildReviewUserPrompt(session, workout, recentSessions)

        val request = ChatRequest(
            model = Models.QUICK_REVIEW,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            ),
            // Reasoning модели (o-series) не поддерживают temperature.
            // Effort medium — баланс между скоростью и глубиной анализа.
            reasoningEffort = "medium",
            responseFormat = ResponseFormat(
                type = "json_schema",
                jsonSchema = JsonSchemaWrapper(
                    name = "quick_review",
                    strict = true,
                    schema = ProgramSchemas.quickReviewSchema()
                )
            )
        )

        android.util.Log.d("Forma.OpenAi", "Requesting quick review for session ${session.id}")
        val response = api.chat(request)
        val content = response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Пустой ответ от OpenAI")

        return try {
            json.decodeFromString(
                com.forma.app.data.remote.dto.QuickReviewDto.serializer(),
                content
            )
        } catch (t: Throwable) {
            android.util.Log.e("Forma.OpenAi", "Failed to parse review response: $content", t)
            throw IllegalStateException("Не удалось распарсить ответ AI: ${t.message}", t)
        }
    }

    /**
     * Обогащение алгоритмических рекомендаций ProgressionEngine.
     *
     * AI получает на вход уже посчитанные числа (новый вес, диапазон, тип действия)
     * и его задача — НЕ выдумывать свои числа, а:
     *  1. Выбрать какие из переданных рекомендаций показать пользователю
     *  2. Сформировать summary разбора
     *  3. Определить общий verdict
     *  4. Переписать rationale в более человеческий тон
     *
     * Это снимает с AI самую опасную задачу — придумывание весов "на глаз".
     */
    suspend fun enrichEngineReview(
        session: com.forma.app.domain.model.WorkoutSession,
        workout: com.forma.app.domain.model.Workout,
        engineRecommendations: List<EnrichmentInput>,
        userSex: com.forma.app.domain.model.Sex,
        wellnessContext: WellnessContext?
    ): com.forma.app.data.remote.dto.QuickReviewDto {
        val systemPrompt = """
            Ты — опытный тренер. Делаешь короткий разбор тренировки пользователя.
            
            ⚠️ ВАЖНО: алгоритмический движок прогрессии УЖЕ посчитал конкретные
            рекомендации с числами. Твоя задача — НЕ ВЫДУМЫВАТЬ свои числа,
            а обогатить готовые рекомендации человеческим тоном.
            
            Что от тебя нужно:
            
            1. SUMMARY (2-3 предложения) — общий обзор тренировки. Опирайся на факты:
               что хорошо получилось, что не пошло, общее впечатление.
            
            2. VERDICT — общий вывод:
               - PROGRESS: есть рост (поднял вес или повторы)
               - REGRESS: есть падение
               - STAGNATION: 3+ тренировок без изменений
               - EARLY: мало данных для сравнения
               - SOLID: стабильная работа в норме
            
            3. ВЫБОР РЕКОМЕНДАЦИЙ — из переданного списка алгоритмических
               рекомендаций возьми те которые стоит показать пользователю.
               КРИТИЧНО: используй ТОЧНО те же числа newWeightKg/newRepsMin/newRepsMax
               что передал движок. Не округляй, не меняй знак, не подменяй.
            
            4. ПЕРЕПИСАТЬ RATIONALE — обоснование можешь сделать человечнее:
               вместо "RIR 0 на 60×4 — закидываем шаг назад" можешь сказать
               "Не дотянул до диапазона повторов, имеет смысл вернуться на шаг назад
               и закрепить технику". Опирайся на факты сессии.
            
            Тон: прямой, второе лицо ("ты"), без воды. Фразы краткие.
            Все тексты на русском.
            
            Учитывай пол пользователя в формулировках (женский тон vs мужской —
            не использовать стереотипных формулировок).

            Если в запросе передан блок WELLNESS_CONTEXT:
            - Если preWorkoutEnergy = "Уставший" ИЛИ sevenDayAvgEnergy < 1.8 —
              пользователь пришёл уставшим. Если есть регрессы или повторы ниже плана,
              не трактуй это как потолок прогресса или автоматический deload.
              Скажи, что день не зашёл, и на следующей неделе можно попробовать тот же вес.
            - Если recentLowEnergyStreak >= 3 — несколько дней подряд низкая энергия.
              Слабая тренировка объяснима восстановлением; не меняй программу агрессивно.
            - Если postWorkoutSleep <= 2 или postWorkoutStress >= 4 — используй это как
              контекст summary. Не давай банальное "выспись"; оцени результат с учётом фона.
            - Если wellness в норме и при этом есть регресс — это более реальный сигнал,
              что вес объективно завышен или нужен deload.
            - Если wellness в норме и есть прогресс — обычная позитивная интерпретация.
            Не упоминай wellness в каждом предложении. Достаточно 1-2 уместных упоминаний.
            
            Если движок передал INSUFFICIENT_DATA для всех — возвращай verdict EARLY
            и одну информационную рекомендацию KEEP про "нужно больше данных".
            
            Отвечай ТОЛЬКО JSON по схеме.
        """.trimIndent()

        val userPrompt = buildEnrichmentUserPrompt(
            session = session,
            workout = workout,
            engineRecs = engineRecommendations,
            userSex = userSex,
            wellnessContext = wellnessContext
        )

        val request = ChatRequest(
            model = Models.QUICK_REVIEW,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            ),
            reasoningEffort = "medium",
            responseFormat = ResponseFormat(
                type = "json_schema",
                jsonSchema = JsonSchemaWrapper(
                    name = "quick_review",
                    strict = true,
                    schema = ProgramSchemas.quickReviewSchema()
                )
            )
        )

        android.util.Log.d("Forma.OpenAi",
            "Enriching ${engineRecommendations.size} engine recommendations for session ${session.id}")
        val response = api.chat(request)
        val content = response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Пустой ответ от OpenAI")

        return try {
            json.decodeFromString(
                com.forma.app.data.remote.dto.QuickReviewDto.serializer(),
                content
            )
        } catch (t: Throwable) {
            android.util.Log.e("Forma.OpenAi", "Failed to parse enrichment: $content", t)
            throw IllegalStateException("Не удалось распарсить ответ AI: ${t.message}", t)
        }
    }

    private fun buildEnrichmentUserPrompt(
        session: com.forma.app.domain.model.WorkoutSession,
        workout: com.forma.app.domain.model.Workout,
        engineRecs: List<EnrichmentInput>,
        userSex: com.forma.app.domain.model.Sex,
        wellnessContext: WellnessContext?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Завершена тренировка: ${workout.title}")
        sb.appendLine("Фокус: ${workout.focus}")
        sb.appendLine("Пол пользователя: ${userSex.displayName}")
        sb.appendLine()

        sb.appendLine("════════ АЛГОРИТМИЧЕСКИЕ РЕКОМЕНДАЦИИ (используй ТОЧНЫЕ числа) ════════")
        engineRecs.forEachIndexed { i, r ->
            sb.appendLine()
            sb.appendLine("[${i + 1}] ${r.exerciseName} (exerciseId=${r.exerciseId})")
            sb.appendLine("    Действие: ${r.action}")
            sb.appendLine("    Уверенность: ${r.confidence}")
            r.newWeightKg?.let { sb.appendLine("    Новый вес: $it кг") }
            r.newRepsMin?.let { sb.appendLine("    Новый мин повторов: $it") }
            r.newRepsMax?.let { sb.appendLine("    Новый макс повторов: $it") }
            sb.appendLine("    Причина (от движка): ${r.engineRationale}")
            if (r.isDisputed) {
                sb.appendLine("    ⚠ DISPUTED: ${r.disputeReason}")
            }
            sb.appendLine("    Сигналы: " +
                "сессий=${r.sessionsAnalyzed}, " +
                "RIR=${r.avgRirLast?.let { "%.1f".format(it) } ?: "—"}, " +
                "повторов=${"%.1f".format(r.avgRepsLast)}, " +
                "вес=${r.workingWeightLast}, " +
                "цель RIR=${r.targetRir}")
        }
        sb.appendLine()

        if (wellnessContext != null) {
            sb.appendLine("════════ WELLNESS_CONTEXT ════════")
            sb.appendLine("Самочувствие до тренировки: ${wellnessContext.preWorkoutEnergy ?: "не указано"}")
            sb.appendLine("Самочувствие после тренировки: ${wellnessContext.postWorkoutEnergy ?: "не указано"}")
            sb.appendLine("Сон прошлой ночью: ${wellnessContext.postWorkoutSleep?.let { "$it/5" } ?: "не указано"}")
            sb.appendLine("Уровень стресса: ${wellnessContext.postWorkoutStress?.let { "$it/5" } ?: "не указано"}")
            sb.appendLine("Настроение: ${wellnessContext.postWorkoutMood?.let { "$it/5" } ?: "не указано"}")
            sb.appendLine("Средняя энергия за 7 дней: ${wellnessContext.sevenDayAvgEnergy?.let { "%.1f".format(it) } ?: "не указано"}")
            sb.appendLine("Уставших дней подряд: ${wellnessContext.recentLowEnergyStreak}")
            sb.appendLine()
        }

        sb.appendLine("════════ ФАКТИЧЕСКИЕ ПОДХОДЫ ════════")
        session.exerciseLogs.forEach { log ->
            sb.appendLine("- ${log.exerciseName}")
            log.sets.filter { it.isCompleted }.forEach { s ->
                val w = s.weightKg?.let { "${it} кг × " } ?: ""
                val rir = s.rir?.let { ", RIR $it" } ?: ""
                sb.appendLine("    ${w}${s.reps}$rir")
            }
        }

        return sb.toString()
    }

    private fun buildReviewUserPrompt(
        session: com.forma.app.domain.model.WorkoutSession,
        workout: com.forma.app.domain.model.Workout,
        recentSessions: List<com.forma.app.domain.model.WorkoutSession>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Только что завершена тренировка: ${workout.title}")
        sb.appendLine("Фокус: ${workout.focus}")
        sb.appendLine()
        sb.appendLine("УПРАЖНЕНИЯ ИЗ ПРОГРАММЫ (используй эти exerciseId):")
        workout.exercises.forEach { ex ->
            sb.appendLine("- exerciseId=${ex.id} | \"${ex.name}\" | целевой ${ex.targetSets}x${ex.targetRepsMin}-${ex.targetRepsMax}, отдых ${ex.restSeconds}с${ex.startingWeightKg?.let { ", старт $it кг" } ?: ""}")
        }
        sb.appendLine()
        sb.appendLine("ФАКТИЧЕСКИ ВЫПОЛНЕНО НА ЭТОЙ ТРЕНИРОВКЕ:")
        session.exerciseLogs.forEach { log ->
            sb.appendLine("- ${log.exerciseName} (exerciseId=${log.exerciseId})")
            log.sets.filter { it.isCompleted }.forEach { s ->
                val weight = s.weightKg?.let { "${it} кг × " } ?: ""
                val rir = s.rir?.let { ", RIR $it" } ?: ""
                sb.appendLine("    подход ${s.setNumber}: ${weight}${s.reps}$rir")
            }
            val skipped = log.sets.count { !it.isCompleted }
            if (skipped > 0) sb.appendLine("    (пропущено подходов: $skipped)")
        }
        if (recentSessions.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("ПРЕДЫДУЩИЕ ТРЕНИРОВКИ ЭТОГО ЖЕ ДНЯ (для сравнения, от старых к новым):")
            recentSessions.sortedBy { it.startedAt }.forEach { prev ->
                val date = java.text.SimpleDateFormat("d MMM", java.util.Locale("ru"))
                    .format(java.util.Date(prev.startedAt))
                sb.appendLine("[$date] объём ${prev.totalVolumeKg.toInt()} кг, выполнено ${prev.completedSetsCount}/${prev.totalSetsCount}")
                prev.exerciseLogs.forEach { log ->
                    val tops = log.sets.filter { it.isCompleted }
                        .maxByOrNull { it.weightKg ?: 0.0 }
                    if (tops != null) {
                        val w = tops.weightKg?.let { "$it кг × " } ?: ""
                        val rir = tops.rir?.let { " RIR $it" } ?: ""
                        sb.appendLine("  ${log.exerciseName}: ${w}${tops.reps}$rir")
                    }
                }
            }
        } else {
            sb.appendLine()
            sb.appendLine("Это первая тренировка этого дня — данных для сравнения нет.")
        }
        return sb.toString()
    }

    private fun buildSystemPrompt(): String = """
        Ты — опытный тренер по силовой и функциональной подготовке.
        Твоя задача — составить недельную программу тренировок строго под параметры пользователя.

        ╔═══════════════════════════════════════════════════════════════╗
        ║  АБСОЛЮТНОЕ ПРАВИЛО: workouts.length === daysPerWeek          ║
        ║  Это правило НИКОГДА не нарушается ни при каких условиях.     ║
        ╚═══════════════════════════════════════════════════════════════╝

        Перед формированием ответа ты ОБЯЗАН:
        1. Прочитать значение поля "Дней в неделю: N" из user-prompt.
        2. Прочитать список "Дни тренировок: X1, X2, ..., XN" — там будет ровно N дней.
        3. Сформировать массив workouts ровно из N элементов, по одному на каждый день.
        4. В title КАЖДОЙ тренировки добавить пометку "(1/N)", "(2/N)", ..., "(N/N)".
           Это нужно тебе чтобы не сбиться со счёта. Например для N=3:
             workouts[0].title = "Грудь и трицепс (1/3)"
             workouts[1].title = "Спина и бицепс (2/3)"
             workouts[2].title = "Ноги и плечи (3/3)"
           ВНИМАНИЕ: пометка "(N/N)" должна быть ПОСЛЕДНЕЙ по счёту.
        5. Каждый workout должен иметь УНИКАЛЬНЫЙ dayOfWeek из списка "Дни тренировок".

        Прочие требования:
        - Названия упражнений и описания — на русском языке.
        - Каждая тренировка минимум 4 упражнения. Длительность близка к sessionDurationMin.
        - Используй ТОЛЬКО оборудование из списка "Доступное оборудование".

        Правила построения программы:
        - Для новичков: 2-3 подхода, базовые движения, простая техника.
        - Для среднего уровня: 3-4 подхода, сплиты по группам мышц.
        - Для продвинутых: 4+ подходов, сложные варианты, явная прогрессия нагрузки.
        - Учитывай ограничения и травмы пользователя — не предлагай опасные упражнения.
        - Для упражнений с собственным весом ставь usesWeight=false.
        - restSeconds: 60-90 для гипертрофии, 120-180 для силы, 30-60 для выносливости.
        - В notes — конкретные технические подсказки (1-2 предложения).
        - Не ставь две тренировки одной и той же группы мышц подряд.

        Отвечай ТОЛЬКО в JSON согласно схеме, без комментариев и пояснений.
    """.trimIndent()

    private fun buildUserPrompt(p: UserProfile): String {
        // Если пользователь не указал предпочтительные дни — равномерно распределяем по неделе.
        // Это критично: иначе AI составит программу только на 1 день, формально считая
        // "любые дни" как один день.
        val days: List<DayOfWeek> = if (p.preferredDays.isNotEmpty()) {
            p.preferredDays.sortedBy { it.ordinal }
        } else {
            spreadDays(p.daysPerWeek)
        }

        val daysStr = days.joinToString(", ") { "${it.full} (${it.name})" }
        val equipment = p.equipment.joinToString(", ") { it.displayName }
        val anthro = listOfNotNull(
            p.age?.let { "возраст $it" },
            p.heightCm?.let { "рост $it см" },
            p.weightKg?.let { "вес $it кг" }
        ).joinToString(", ").ifBlank { "не указаны" }
        val lim = p.limitations?.takeIf { it.isNotBlank() } ?: "нет"

        return """
            Составь недельную программу тренировок.

            ╔═══════════════════════════════════════════╗
            ║  Дней в неделю: ${p.daysPerWeek}                          ║
            ║  Массив workouts должен иметь РОВНО ${p.daysPerWeek}      ║
            ║  элементов. Не больше, не меньше.         ║
            ╚═══════════════════════════════════════════╝

            Дни тренировок (${days.size} штук): $daysStr

            Чек-лист перед отправкой ответа:
            [ ] В массиве workouts ровно ${p.daysPerWeek} элементов
            [ ] У каждого title в конце пометка "(N/${p.daysPerWeek})"
            [ ] У каждого workout уникальный dayOfWeek из списка выше

            Параметры пользователя:
            - Цель: ${p.goal.displayName}
            - Уровень: ${p.level.displayName}
            - Пол: ${p.sex.displayName}
            - Длительность сессии: ${p.sessionDurationMin} минут
            - Доступное оборудование: $equipment
            - Антропометрия: $anthro
            - Ограничения: $lim
        """.trimIndent()
    }

    /**
     * Равномерно распределяет N тренировочных дней по неделе.
     * Для популярных значений возвращает оптимальные сплиты с разрывами на восстановление.
     */
    private fun spreadDays(count: Int): List<DayOfWeek> {
        val all = DayOfWeek.entries
        return when (count.coerceIn(1, 7)) {
            1 -> listOf(DayOfWeek.WED)
            2 -> listOf(DayOfWeek.MON, DayOfWeek.THU)
            3 -> listOf(DayOfWeek.MON, DayOfWeek.WED, DayOfWeek.FRI)
            4 -> listOf(DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.THU, DayOfWeek.FRI)
            5 -> listOf(DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED, DayOfWeek.FRI, DayOfWeek.SAT)
            6 -> listOf(DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED, DayOfWeek.THU, DayOfWeek.FRI, DayOfWeek.SAT)
            7 -> all.toList()
            else -> listOf(DayOfWeek.MON, DayOfWeek.WED, DayOfWeek.FRI)
        }
    }
}
