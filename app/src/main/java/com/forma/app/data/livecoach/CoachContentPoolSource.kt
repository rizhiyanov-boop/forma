package com.forma.app.data.livecoach

import com.forma.app.domain.livecoach.CoachContentPool

interface CoachContentPoolSource {
    suspend fun loadPool(contentId: String): CoachContentPool?
}
