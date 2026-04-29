package com.forma.app.domain.livecoach

interface CoachContentRepository {
    /**
     * Получить случайную единицу контента для упражнения.
     * Учитывает историю показов — не возвращает то что показывали недавно.
     * Возвращает null если для упражнения нет контента.
     */
    suspend fun pickContentForExercise(
        contentId: String,
        preferredType: CoachContentType? = null,
        excludedIds: Set<String> = emptySet()
    ): CoachContentItem?

    /**
     * Записать что данная единица контента была показана.
     * Используется для предотвращения повторов в ближайшие N недель.
     */
    suspend fun markShown(contentItemId: String)
}
