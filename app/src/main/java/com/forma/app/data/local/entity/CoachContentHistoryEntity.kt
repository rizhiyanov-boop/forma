package com.forma.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "coach_content_history",
    indices = [Index("contentItemId"), Index("shownAt")]
)
data class CoachContentHistoryEntity(
    @PrimaryKey val id: String,
    val contentItemId: String,
    val shownAt: Long
)
