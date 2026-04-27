package com.forma.app.data.remote

/**
 * Плоское представление EngineRecommendation для передачи в AI-промпт.
 * Содержит всё что нужно AI чтобы написать человеческое объяснение,
 * но НЕ включает Kotlin-объекты — только примитивы.
 *
 * Используется в OpenAiService.enrichEngineReview().
 */
data class EnrichmentInput(
    val exerciseId: String,
    val exerciseName: String,
    val action: String,                // ProgressionAction.name
    val confidence: String,            // Confidence.name
    val newWeightKg: Double?,
    val newRepsMin: Int?,
    val newRepsMax: Int?,
    val engineRationale: String,
    val isDisputed: Boolean,
    val disputeReason: String?,
    // Сигналы — ключевые данные для AI понимания контекста
    val sessionsAnalyzed: Int,
    val avgRirLast: Double?,
    val avgRepsLast: Double,
    val workingWeightLast: Double,
    val targetRir: Int
)
