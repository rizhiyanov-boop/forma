package com.forma.app.data.repository

import com.forma.app.data.local.dao.UserProfileDao
import com.forma.app.data.local.entity.UserProfileEntity
import com.forma.app.domain.model.UserProfile
import com.forma.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val dao: UserProfileDao
) : UserProfileRepository {

    override fun observeProfile(): Flow<UserProfile?> =
        dao.observe().map { it?.toDomain() }

    override suspend fun getProfile(): UserProfile? =
        dao.get()?.toDomain()

    override suspend fun saveProfile(profile: UserProfile) {
        dao.upsert(UserProfileEntity.fromDomain(profile))
    }

    override suspend fun clear() = dao.clear()
}
