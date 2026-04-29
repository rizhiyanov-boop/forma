package com.forma.app.domain.livecoach

import kotlinx.serialization.Serializable

@Serializable
enum class CoachContentType(val displayName: String) {
    TECHNIQUE("Техника"),
    MISTAKE("Ошибка"),
    MOTIVATION("Установка"),
    FACT("Факт"),
    TIP("Подсказка")
}

@Serializable
data class CoachContentItem(
    val id: String,
    val type: CoachContentType,
    val text: String,
    val weight: Double = 1.0
)

@Serializable
data class CoachContentPool(
    val contentKey: String,
    val name: String,
    val version: Int = 1,
    val content: List<CoachContentItem>
)
