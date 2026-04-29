package com.forma.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class QuickReviewDto(
    val verdict: String,           // PROGRESS / STAGNATION / REGRESS / EARLY / SOLID
    val summary: String,
    val recommendations: List<RecommendationDto>
)

@Serializable
data class RecommendationDto(
    val type: String,              // INCREASE_WEIGHT, etc.
    val title: String,
    val rationale: String,
    val applicableExerciseIds: List<String>,
    val newWeightKg: Double?,
    val newRepsMin: Int?,
    val newRepsMax: Int?,
    val newRestSeconds: Int?
)
