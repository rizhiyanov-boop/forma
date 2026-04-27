package com.forma.app.domain.review

import kotlinx.serialization.Serializable

/**
 * Вердикт AI по тренировке/периоду.
 */
@Serializable
enum class Verdict(val displayName: String) {
    PROGRESS("Прогресс"),
    STAGNATION("Застой"),
    REGRESS("Регресс"),
    EARLY("Слишком рано"),       // мало данных для вердикта
    SOLID("Стабильно")           // нейтральная норма
}

/**
 * Тип рекомендации — определяет что именно изменить в программе.
 */
@Serializable
enum class RecommendationType {
    INCREASE_WEIGHT,    // поднять рабочий вес упражнения
    DECREASE_WEIGHT,    // снизить вес (после регресса)
    CHANGE_REPS,        // изменить целевой диапазон повторов
    DELOAD,             // разгрузочная неделя — снизить интенсивность
    KEEP,               // ничего не менять, держать линию
    TECHNIQUE,          // совет по технике, без изменения программы
    REST_LONGER,        // увеличить отдых между подходами
    REST_SHORTER,       // уменьшить отдых
    INFO                // просто инфо/подсветка для пользователя
}

/**
 * Конкретная рекомендация AI.
 *
 * payload — параметры применения. Для INCREASE_WEIGHT это новый вес.
 * Для CHANGE_REPS — новый диапазон. Для большинства типов опционально.
 *
 * applicableTo — exercise ids в активной программе. Может быть пустым,
 * если рекомендация общая (DELOAD, INFO).
 */
@Serializable
data class Recommendation(
    val id: String,
    val type: RecommendationType,
    val title: String,                              // "Подними жим лёжа на 2.5 кг"
    val rationale: String,                          // "65×6 RIR 0-1 — готов к прогрессии"
    val applicableExerciseIds: List<String> = emptyList(),
    val newWeightKg: Double? = null,                // для INCREASE/DECREASE_WEIGHT
    val newRepsMin: Int? = null,                    // для CHANGE_REPS
    val newRepsMax: Int? = null,                    // для CHANGE_REPS
    val newRestSeconds: Int? = null                 // для REST_LONGER/SHORTER
)

/**
 * Полный разбор — вердикт + текст + рекомендации + статусы.
 */
@Serializable
data class WorkoutReview(
    val id: String,
    val createdAt: Long,
    val sessionId: String?,                         // null = периодический разбор без привязки
    val triggerType: ReviewTrigger,
    val verdict: Verdict,
    val summary: String,                            // короткое объяснение вердикта
    val recommendations: List<Recommendation>,
    val isApplied: Boolean = false,                 // были ли применены изменения
    val isDismissed: Boolean = false,               // отклонены ли все рекомендации
    val appliedRecommendationIds: List<String> = emptyList()
)

@Serializable
enum class ReviewTrigger {
    AFTER_WORKOUT,     // быстрый разбор после конкретной сессии
    WEEKLY,            // полный недельный разбор
    MANUAL             // юзер сам нажал "Разбор сейчас"
}
