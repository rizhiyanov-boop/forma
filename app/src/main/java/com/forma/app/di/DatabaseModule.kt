package com.forma.app.di

import android.content.Context
import androidx.room.Room
import com.forma.app.data.local.FormaDatabase
import com.forma.app.data.local.dao.ExerciseOverrideDao
import com.forma.app.data.local.dao.ProgramDao
import com.forma.app.data.local.dao.ReviewDao
import com.forma.app.data.local.dao.SessionDao
import com.forma.app.data.local.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): FormaDatabase =
        Room.databaseBuilder(ctx, FormaDatabase::class.java, "forma.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideProfileDao(db: FormaDatabase): UserProfileDao = db.userProfileDao()
    @Provides fun provideProgramDao(db: FormaDatabase): ProgramDao = db.programDao()
    @Provides fun provideSessionDao(db: FormaDatabase): SessionDao = db.sessionDao()
    @Provides fun provideOverrideDao(db: FormaDatabase): ExerciseOverrideDao = db.overrideDao()
    @Provides fun provideReviewDao(db: FormaDatabase): ReviewDao = db.reviewDao()
}
