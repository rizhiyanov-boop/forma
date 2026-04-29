package com.forma.app.data.local.converter

import androidx.room.TypeConverter
import com.forma.app.domain.model.DayOfWeek
import com.forma.app.domain.model.Equipment
import com.forma.app.domain.model.ExperienceLevel
import com.forma.app.domain.model.Sex
import com.forma.app.domain.model.Goal
import com.forma.app.domain.model.MuscleGroup

class Converters {

    private val sep = "|"

    @TypeConverter fun goalToString(v: Goal): String = v.name
    @TypeConverter fun stringToGoal(v: String): Goal = Goal.valueOf(v)

    @TypeConverter fun levelToString(v: ExperienceLevel): String = v.name
    @TypeConverter fun stringToLevel(v: String): ExperienceLevel = ExperienceLevel.valueOf(v)

    @TypeConverter fun sexToString(v: Sex): String = v.name
    @TypeConverter fun stringToSex(v: String): Sex = Sex.valueOf(v)

    @TypeConverter fun dayToString(v: DayOfWeek): String = v.name
    @TypeConverter fun stringToDay(v: String): DayOfWeek = DayOfWeek.valueOf(v)

    @TypeConverter fun muscleToString(v: MuscleGroup): String = v.name
    @TypeConverter fun stringToMuscle(v: String): MuscleGroup = MuscleGroup.valueOf(v)

    @TypeConverter
    fun daysListToString(list: List<DayOfWeek>): String =
        list.joinToString(sep) { it.name }

    @TypeConverter
    fun stringToDaysList(v: String): List<DayOfWeek> =
        if (v.isBlank()) emptyList() else v.split(sep).map { DayOfWeek.valueOf(it) }

    @TypeConverter
    fun equipmentListToString(list: List<Equipment>): String =
        list.joinToString(sep) { it.name }

    @TypeConverter
    fun stringToEquipmentList(v: String): List<Equipment> =
        if (v.isBlank()) emptyList() else v.split(sep).map { Equipment.valueOf(it) }

    @TypeConverter
    fun musclesListToString(list: List<MuscleGroup>): String =
        list.joinToString(sep) { it.name }

    @TypeConverter
    fun stringToMusclesList(v: String): List<MuscleGroup> =
        if (v.isBlank()) emptyList() else v.split(sep).map { MuscleGroup.valueOf(it) }
}
