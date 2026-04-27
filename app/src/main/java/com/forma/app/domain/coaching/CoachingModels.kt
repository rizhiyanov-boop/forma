package com.forma.app.domain.coaching

/**
 * Тип решения движка прогрессии для конкретного упражнения.
 */
enum class ProgressionAction {
    INCREASE_WEIGHT,    // Готов к увеличению нагрузки
    INCREASE_REPS,      // На том же весе, но больше повторов (двойная прогрессия)
    KEEP,               // Оставить как есть, рано что-то менять
    DECREASE_WEIGHT,    // Снизить вес после регресса
    DELOAD,             // Разгрузка — вес ×0.85 + повторы −2
    INSUFFICIENT_DATA   // Меньше 1 завершённой сессии этого упражнения
}

/**
 * Уровень уверенности в рекомендации. Влияет на UI: LOW можно скрывать или
 * подсвечивать как «предположение», HIGH применять автоматически.
 */
enum class Confidence { LOW, MEDIUM, HIGH }

/**
 * Алгоритмическая рекомендация для одного упражнения. Содержит конкретные
 * числа — AI не выдумывает свои, только обогащает текст вокруг.
 *
 * Если action не предполагает изменения параметра, соответствующее поле = null.
 */
data class EngineRecommendation(
    val exerciseId: String,
    val exerciseName: String,
    val action: ProgressionAction,
    val confidence: Confidence,

    /** Новые параметры для применения. */
    val newWeightKg: Double? = null,
    val newRepsMin: Int? = null,
    val newRepsMax: Int? = null,

    /**
     * Краткое объяснение в формате "65×6 RIR 0-1 в трёх тренировках подряд".
     * Это feed для AI: он добавляет нюансы тоном.
     */
    val rationale: String,

    /**
     * Внутренние сигналы для дебага/диагностики. UI не показывает.
     */
    val signals: ProgressionSignals
)

/**
 * Снимок ключевых сигналов которые движок учёл при принятии решения.
 * Удобно для логирования и юнит-тестов — можно проверить что движок
 * "увидел" нужные паттерны.
 */
data class ProgressionSignals(
    val sessionsAnalyzed: Int,
    val avgRirLast: Double?,
    val avgRepsLast: Double,
    val workingWeightLast: Double,
    val targetRir: Int,
    val rirOffset: Double?,           // avgRir - targetRir, может быть null
    val hitTopOfRange: Boolean,       // повторы достигли верхней границы
    val sessionsAtSameWeight: Int,    // сколько подряд тренировок на том же весе
    val regressStreak: Int            // сколько подряд тренировок с падением
)
