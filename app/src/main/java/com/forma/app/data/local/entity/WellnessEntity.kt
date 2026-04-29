package com.forma.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wellness_log",
    indices = [
        Index("sessionId"),
        Index("timestamp"),
        Index("type")
    ]
)
data class WellnessEntity(
    @PrimaryKey val id: String,
    val sessionId: String?,
    val timestamp: Long,
    val type: String,
    val energy: String?,
    val sleepQuality: Int?,
    val stressLevel: Int?,
    val mood: Int?,
    val notes: String?
)
