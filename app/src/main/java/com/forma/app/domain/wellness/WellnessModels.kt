package com.forma.app.domain.wellness

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class WellnessTriggerType {
    PRE_WORKOUT,
    BETWEEN_EXERCISES,
    POST_WORKOUT,
    DAILY
}

@Serializable
enum class EnergyLevel(val displayName: String) {
    FATIGUED("Уставший"),
    NORMAL("Норма"),
    ENERGIZED("Бодрый")
}

@Serializable
data class WellnessEntry(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String?,
    val timestamp: Long,
    val type: WellnessTriggerType,
    val energy: EnergyLevel? = null,
    val sleepQuality: Int? = null,
    val stressLevel: Int? = null,
    val mood: Int? = null,
    val notes: String? = null
)
