package com.forma.app.domain.review

import kotlinx.coroutines.flow.Flow

interface ReviewRepository {

    /**
     * Запускает быстрый AI-разбор после конкретной сессии.
     * Возвращает id созданного разбора. Если уже есть разбор для этой сессии — возвращает его.
     */
    suspend fun reviewAfterWorkout(sessionId: String): String

    /**
     * Реактивно наблюдает за конкретным разбором.
     */
    fun observeReview(reviewId: String): Flow<WorkoutReview?>

    /**
     * Последние разборы (для истории).
     */
    fun observeRecentReviews(limit: Int = 20): Flow<List<WorkoutReview>>

    /**
     * Применить одну рекомендацию — изменит программу/вес/повторы соответственно.
     */
    suspend fun applyRecommendation(reviewId: String, recommendationId: String)

    /**
     * Отметить разбор как просмотренный/отклонённый — пользователь увидел и не хочет применять.
     */
    suspend fun dismissReview(reviewId: String)
}
