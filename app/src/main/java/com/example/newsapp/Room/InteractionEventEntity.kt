package com.example.newsapp.Room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interaction_events")
data class InteractionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val articleId: String,
    val interactionType: String,
    val timestamp: Long
)
