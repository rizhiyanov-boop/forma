package com.forma.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Goal(val displayName: String) {
    MUSCLE_GAIN("Набор мышечной массы"),
    STRENGTH("Сила"),
    FAT_LOSS("Жиросжигание"),
    ENDURANCE("Выносливость"),
    GENERAL_FITNESS("Общий тонус")
}

@Serializable
enum class ExperienceLevel(val displayName: String) {
    BEGINNER("Новичок"),
    INTERMEDIATE("Средний"),
    ADVANCED("Продвинутый")
}

@Serializable
enum class Sex(val displayName: String) {
    MALE("Мужской"),
    FEMALE("Женский"),
    UNSPECIFIED("Не указывать")
}

@Serializable
enum class Equipment(val displayName: String) {
    BARBELL("Штанга"),
    DUMBBELLS("Гантели"),
    MACHINES("Тренажёры"),
    PULLUP_BAR("Турник"),
    BENCH("Скамья"),
    CABLES("Блочные тренажёры"),
    KETTLEBELLS("Гири"),
    BODYWEIGHT("Своё тело"),
    CARDIO("Кардиотренажёры")
}

@Serializable
enum class DayOfWeek(val short: String, val full: String) {
    MON("Пн", "Понедельник"),
    TUE("Вт", "Вторник"),
    WED("Ср", "Среда"),
    THU("Чт", "Четверг"),
    FRI("Пт", "Пятница"),
    SAT("Сб", "Суббота"),
    SUN("Вс", "Воскресенье")
}

@Serializable
data class UserProfile(
    val goal: Goal,
    val level: ExperienceLevel,
    val sex: Sex = Sex.UNSPECIFIED,
    val daysPerWeek: Int,
    val preferredDays: List<DayOfWeek>,
    val equipment: List<Equipment>,
    val heightCm: Int?,
    val weightKg: Double?,
    val age: Int?,
    val limitations: String?,
    val sessionDurationMin: Int = 60
)
