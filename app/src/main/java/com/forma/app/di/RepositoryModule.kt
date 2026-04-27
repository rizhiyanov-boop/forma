package com.forma.app.di

import com.forma.app.data.analytics.AnalyticsRepositoryImpl
import com.forma.app.data.backup.BackupRepositoryImpl
import com.forma.app.data.repository.ProgramRepositoryImpl
import com.forma.app.data.repository.ReviewRepositoryImpl
import com.forma.app.data.repository.SessionRepositoryImpl
import com.forma.app.data.repository.UserProfileRepositoryImpl
import com.forma.app.domain.analytics.AnalyticsRepository
import com.forma.app.domain.backup.BackupRepository
import com.forma.app.domain.repository.ProgramRepository
import com.forma.app.domain.repository.SessionRepository
import com.forma.app.domain.repository.UserProfileRepository
import com.forma.app.domain.review.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindUserProfileRepo(impl: UserProfileRepositoryImpl): UserProfileRepository

    @Binds @Singleton
    abstract fun bindProgramRepo(impl: ProgramRepositoryImpl): ProgramRepository

    @Binds @Singleton
    abstract fun bindSessionRepo(impl: SessionRepositoryImpl): SessionRepository

    @Binds @Singleton
    abstract fun bindAnalyticsRepo(impl: AnalyticsRepositoryImpl): AnalyticsRepository

    @Binds @Singleton
    abstract fun bindReviewRepo(impl: ReviewRepositoryImpl): ReviewRepository

    @Binds @Singleton
    abstract fun bindBackupRepo(impl: BackupRepositoryImpl): BackupRepository
}
