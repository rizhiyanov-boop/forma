package com.forma.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.forma.app.data.local.converter.Converters
import com.forma.app.data.local.dao.CoachContentHistoryDao
import com.forma.app.data.local.dao.ExerciseOverrideDao
import com.forma.app.data.local.dao.ProgramDao
import com.forma.app.data.local.dao.ReviewDao
import com.forma.app.data.local.dao.SessionDao
import com.forma.app.data.local.dao.SetReactionDao
import com.forma.app.data.local.dao.UserProfileDao
import com.forma.app.data.local.dao.WellnessDao
import com.forma.app.data.local.entity.CoachContentHistoryEntity
import com.forma.app.data.local.entity.ExerciseEntity
import com.forma.app.data.local.entity.ExerciseLogEntity
import com.forma.app.data.local.entity.ExerciseOverrideEntity
import com.forma.app.data.local.entity.ProgramEntity
import com.forma.app.data.local.entity.ReviewEntity
import com.forma.app.data.local.entity.SetReactionEntity
import com.forma.app.data.local.entity.SetLogEntity
import com.forma.app.data.local.entity.UserProfileEntity
import com.forma.app.data.local.entity.WorkoutEntity
import com.forma.app.data.local.entity.WorkoutSessionEntity
import com.forma.app.data.local.entity.WellnessEntity

@Database(
    entities = [
        UserProfileEntity::class,
        ProgramEntity::class,
        WorkoutEntity::class,
        ExerciseEntity::class,
        ExerciseOverrideEntity::class,
        CoachContentHistoryEntity::class,
        WorkoutSessionEntity::class,
        ExerciseLogEntity::class,
        SetLogEntity::class,
        ReviewEntity::class,
        WellnessEntity::class,
        SetReactionEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FormaDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun programDao(): ProgramDao
    abstract fun sessionDao(): SessionDao
    abstract fun overrideDao(): ExerciseOverrideDao
    abstract fun reviewDao(): ReviewDao
    abstract fun coachContentHistoryDao(): CoachContentHistoryDao
    abstract fun wellnessDao(): WellnessDao
    abstract fun setReactionDao(): SetReactionDao
}
