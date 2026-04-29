package com.forma.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Ответ AI на запрос замены упражнения — список вариантов.
 */
@Serializable
data class ReplacementSuggestionsDto(
    val suggestions: List<ExerciseDto>,
    val reasoning: String
)
