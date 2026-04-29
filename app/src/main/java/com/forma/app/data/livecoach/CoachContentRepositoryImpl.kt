package com.forma.app.data.livecoach

import com.forma.app.data.local.dao.CoachContentHistoryDao
import com.forma.app.data.local.entity.CoachContentHistoryEntity
import com.forma.app.domain.livecoach.CoachContentItem
import com.forma.app.domain.livecoach.CoachContentRepository
import com.forma.app.domain.livecoach.CoachContentType
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class CoachContentRepositoryImpl @Inject constructor(
    private val assetLoader: CoachContentPoolSource,
    private val historyDao: CoachContentHistoryDao
) : CoachContentRepository {

    override suspend fun pickContentForExercise(
        contentId: String,
        preferredType: CoachContentType?,
        excludedIds: Set<String>
    ): CoachContentItem? {
        val pool = assetLoader.loadPool(contentId) ?: return null
        if (pool.content.isEmpty()) return null

        val recencyThreshold = System.currentTimeMillis() - TWENTY_EIGHT_DAYS_MS
        val recentlyShown = historyDao.shownSince(recencyThreshold).toSet()
        val byType = preferredType?.let { type ->
            pool.content.filter { it.type == type }
        } ?: pool.content
        if (byType.isEmpty()) return null

        val withoutExcluded = byType.filter { it.id !in excludedIds }
        if (withoutExcluded.isEmpty()) return null
        val candidates = withoutExcluded.filter { it.id !in recentlyShown }
        val finalCandidates = candidates.ifEmpty { withoutExcluded }

        val weighted = finalCandidates.map { item ->
            val typeWeight = when (item.type) {
                CoachContentType.TECHNIQUE -> 0.30
                CoachContentType.MISTAKE -> 0.25
                CoachContentType.MOTIVATION -> 0.20
                CoachContentType.FACT -> 0.15
                CoachContentType.TIP -> 0.10
            }
            item to (typeWeight * item.weight)
        }

        val totalWeight = weighted.sumOf { it.second }
        if (totalWeight <= 0.0) return finalCandidates.random()

        var roll = Random.nextDouble(totalWeight)
        for ((item, weight) in weighted) {
            roll -= weight
            if (roll <= 0) return item
        }
        return weighted.last().first
    }

    override suspend fun markShown(contentItemId: String) {
        historyDao.insert(
            CoachContentHistoryEntity(
                id = UUID.randomUUID().toString(),
                contentItemId = contentItemId,
                shownAt = System.currentTimeMillis()
            )
        )
    }

    private companion object {
        const val TWENTY_EIGHT_DAYS_MS = 28L * 24L * 60L * 60L * 1000L
    }
}
