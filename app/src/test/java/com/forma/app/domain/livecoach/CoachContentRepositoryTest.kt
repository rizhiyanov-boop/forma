package com.forma.app.domain.livecoach

import com.forma.app.data.livecoach.CoachContentPoolSource
import com.forma.app.data.livecoach.CoachContentRepositoryImpl
import com.forma.app.data.local.dao.CoachContentHistoryDao
import com.forma.app.data.local.entity.CoachContentHistoryEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CoachContentRepositoryTest {

    @Test
    fun pick_returnsNull_whenPoolMissing() = runBlocking {
        val repo = CoachContentRepositoryImpl(
            assetLoader = FakePoolSource(emptyMap()),
            historyDao = FakeHistoryDao()
        )

        val result = repo.pickContentForExercise("unknown-id")

        assertThat(result).isNull()
    }

    @Test
    fun pick_returnsItem_fromExistingPool() = runBlocking {
        val pool = poolWithItems("bench-press")
        val repo = CoachContentRepositoryImpl(
            assetLoader = FakePoolSource(mapOf("bench-press" to pool)),
            historyDao = FakeHistoryDao()
        )

        val result = repo.pickContentForExercise("bench-press")

        assertThat(result).isNotNull()
        assertThat(pool.content.map { it.id }).contains(result!!.id)
    }

    @Test
    fun pick_excludesRecentlyShown_items() = runBlocking {
        val pool = poolWithItems("bench-press")
        val now = System.currentTimeMillis()
        val history = FakeHistoryDao().apply {
            rows += CoachContentHistoryEntity("1", "item-1", now)
            rows += CoachContentHistoryEntity("2", "item-2", now)
            rows += CoachContentHistoryEntity("3", "item-3", now)
            rows += CoachContentHistoryEntity("4", "item-4", now)
        }
        val repo = CoachContentRepositoryImpl(
            assetLoader = FakePoolSource(mapOf("bench-press" to pool)),
            historyDao = history
        )

        val result = repo.pickContentForExercise("bench-press")

        assertThat(result?.id).isEqualTo("item-5")
    }

    @Test
    fun pick_fallsBackToFullPool_whenAllRecentlyShown() = runBlocking {
        val pool = poolWithItems("bench-press")
        val now = System.currentTimeMillis()
        val history = FakeHistoryDao().apply {
            pool.content.forEachIndexed { index, item ->
                rows += CoachContentHistoryEntity(
                    id = "h-${index + 1}",
                    contentItemId = item.id,
                    shownAt = now
                )
            }
        }
        val repo = CoachContentRepositoryImpl(
            assetLoader = FakePoolSource(mapOf("bench-press" to pool)),
            historyDao = history
        )

        val result = repo.pickContentForExercise("bench-press")

        assertThat(result).isNotNull()
        assertThat(pool.content.map { it.id }).contains(result!!.id)
    }

    @Test
    fun markShown_writesToDao() = runBlocking {
        val history = FakeHistoryDao()
        val repo = CoachContentRepositoryImpl(
            assetLoader = FakePoolSource(emptyMap()),
            historyDao = history
        )

        repo.markShown("item-123")

        assertThat(history.rows).hasSize(1)
        assertThat(history.rows.first().contentItemId).isEqualTo("item-123")
    }

    @Test
    fun pick_respectsTypeAndExcludedIds() = runBlocking {
        val pool = poolWithItems("bench-press")
        val repo = CoachContentRepositoryImpl(
            assetLoader = FakePoolSource(mapOf("bench-press" to pool)),
            historyDao = FakeHistoryDao()
        )

        val result = repo.pickContentForExercise(
            contentId = "bench-press",
            preferredType = CoachContentType.FACT,
            excludedIds = setOf("item-4")
        )

        assertThat(result).isNull()
    }

    private fun poolWithItems(exerciseId: String): CoachContentPool = CoachContentPool(
        contentKey = exerciseId,
        name = "Test pool",
        version = 1,
        content = listOf(
            CoachContentItem("item-1", CoachContentType.TECHNIQUE, "t1"),
            CoachContentItem("item-2", CoachContentType.MISTAKE, "t2"),
            CoachContentItem("item-3", CoachContentType.MOTIVATION, "t3"),
            CoachContentItem("item-4", CoachContentType.FACT, "t4"),
            CoachContentItem("item-5", CoachContentType.TIP, "t5")
        )
    )

    private class FakePoolSource(
        private val pools: Map<String, CoachContentPool>
    ) : CoachContentPoolSource {
        override suspend fun loadPool(contentId: String): CoachContentPool? = pools[contentId]
    }

    private class FakeHistoryDao : CoachContentHistoryDao {
        val rows = mutableListOf<CoachContentHistoryEntity>()

        override suspend fun insert(entity: CoachContentHistoryEntity) {
            rows += entity
        }

        override suspend fun shownSince(since: Long): List<String> =
            rows.filter { it.shownAt >= since }.map { it.contentItemId }

        override suspend fun deleteAll() {
            rows.clear()
        }
    }
}
