package com.forma.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.forma.app.domain.model.DayOfWeek
import com.forma.app.domain.model.Equipment
import com.forma.app.domain.model.ExperienceLevel
import com.forma.app.domain.model.Goal
import com.forma.app.domain.model.Sex
import com.forma.app.domain.model.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
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
    val sessionDurationMin: Int,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = UserProfile(
        goal = goal,
        level = level,
        sex = sex,
        daysPerWeek = daysPerWeek,
        preferredDays = preferredDays,
        equipment = equipment,
        heightCm = heightCm,
        weightKg = weightKg,
        age = age,
        limitations = limitations,
        sessionDurationMin = sessionDurationMin
    )

    companion object {
        fun fromDomain(profile: UserProfile) = UserProfileEntity(
            goal = profile.goal,
            level = profile.level,
            sex = profile.sex,
            daysPerWeek = profile.daysPerWeek,
            preferredDays = profile.preferredDays,
            equipment = profile.equipment,
            heightCm = profile.heightCm,
            weightKg = profile.weightKg,
            age = profile.age,
            limitations = profile.limitations,
            sessionDurationMin = profile.sessionDurationMin
        )
    }
}
