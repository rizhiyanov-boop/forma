package com.forma.app.domain.analytics

import kotlinx.coroutines.flow.Flow

/**
 * Считает агрегаты для экрана графиков.
 * Реактивно реагирует на новые сессии — Flow эмитит обновлённые данные.
 */
interface AnalyticsRepository {

    /**
     * Полный набор агрегатов за последние weeksBack недель.
     * Эмитится при изменении любой сессии в этом окне.
     */
    fun observeProgress(weeksBack: Int = 8): Flow<ProgressData>
}
