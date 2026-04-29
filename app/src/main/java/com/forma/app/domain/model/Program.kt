package com.forma.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class MuscleGroup(val displayName: String) {
    CHEST("Грудь"),
    BACK("Спина"),
    SHOULDERS("Плечи"),
    BICEPS("Бицепс"),
    TRICEPS("Трицепс"),
    FOREARMS("Предплечья"),
    CORE("Кор"),
    QUADS("Квадрицепс"),
    HAMSTRINGS("Бицепс бедра"),
    GLUTES("Ягодицы"),
    CALVES("Икры"),
    FULL_BODY("Всё тело"),
    CARDIO("Кардио")
}

/**
 * Целевой подход с явно заданными параметрами.
 * Используется когда подходы НЕ одинаковые — например, разогрев + рабочий + топ-сет.
 */
@Serializable
data class TargetSet(
    val setNumber: Int,
    val weightKg: Double?,
    val repsMin: Int,
    val repsMax: Int,
    val rirTarget: Int? = null,
    val isOptional: Boolean = false,
    val note: String? = null
)

/**
 * Упражнение в рамках тренировки — с целевыми подходами и повторами.
 * Фактические данные пишутся в SetLog.
 */
@Serializable
data class Exercise(
    val id: String = UUID.randomUUID().toString(),
    val contentId: String? = null,
    val name: String,
    val description: String?,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val equipment: List<Equipment> = emptyList(),
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val restSeconds: Int = 90,
    val usesWeight: Boolean = true,
    val notes: String? = null,
    val orderIndex: Int = 0,

    // Стартовый рабочий вес — подставляется в поле «Вес» при создании сессии.
    // null = просим юзера ввести вручную.
    val startingWeightKg: Double? = null,

    // Если задано — описание подходов берётся отсюда (для сценариев с разными весами).
    // Если null — все подходы одинаковые (targetSets × targetRepsMin..targetRepsMax).
    val targetSetsDetailed: List<TargetSet>? = null
)

@Serializable
data class Workout(
    val id: String = UUID.randomUUID().toString(),
    val programId: String,
    val dayOfWeek: DayOfWeek,
    val title: String,
    val focus: String,               // например "Грудь + Трицепс"
    val estimatedMinutes: Int,
    val exercises: List<Exercise>
)

@Serializable
data class Program(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val weekNumber: Int = 1,
    val profileSnapshot: UserProfile,
    val workouts: List<Workout>
)
