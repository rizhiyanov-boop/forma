package com.forma.app.data.livecoach

import android.content.Context
import android.util.Log
import com.forma.app.domain.livecoach.CoachContentPool
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoachContentAssetLoader @Inject constructor(
    @ApplicationContext private val context: Context
) : CoachContentPoolSource {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, CoachContentPool?>()

    /**
     * Читает JSON для упражнения из assets/coach_content/{contentId}.json.
     * Кэширует результат в памяти. Возвращает null если файла нет или невалидный JSON.
     */
    override suspend fun loadPool(contentId: String): CoachContentPool? {
        if (cache.containsKey(contentId)) return cache[contentId]

        return try {
            val path = "coach_content/$contentId.json"
            val text = context.assets.open(path).bufferedReader().use { it.readText() }
            val pool = json.decodeFromString(CoachContentPool.serializer(), text)
            cache[contentId] = pool
            pool
        } catch (e: Exception) {
            Log.w("Forma.CoachContent", "Failed to load pool for $contentId: ${e.message}")
            cache[contentId] = null
            null
        }
    }
}
