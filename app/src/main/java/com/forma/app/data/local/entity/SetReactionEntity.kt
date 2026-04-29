package com.forma.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_reactions",
    indices = [Index("sessionId"), Index("exerciseId"), Index("createdAt")]
)
data class SetReactionEntity(
    @PrimaryKey val setLogId: String,
    val sessionId: String,
    val exerciseId: String,
    val verdict: String,
    val nextSuggestionType: String?,
    val nextSuggestedWeight: Double?,
    val userAcceptedSuggestion: Boolean?,
    val createdAt: Long
)
