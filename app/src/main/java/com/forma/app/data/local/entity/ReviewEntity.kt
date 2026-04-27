package com.forma.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_reviews",
    indices = [Index("sessionId"), Index("createdAt")]
)
data class ReviewEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val sessionId: String?,
    val triggerType: String,                  // ReviewTrigger.name
    val verdict: String,                      // Verdict.name
    val summary: String,
    /** JSON-сериализованный List<Recommendation>. */
    val recommendationsJson: String,
    val isApplied: Boolean = false,
    val isDismissed: Boolean = false,
    /** JSON-сериализованный List<String> id применённых рекомендаций. */
    val appliedRecommendationIdsJson: String = "[]",
    val isDirty: Boolean = true,
    val syncedAt: Long? = null
)
