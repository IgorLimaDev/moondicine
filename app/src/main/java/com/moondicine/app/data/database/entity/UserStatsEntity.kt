package com.moondicine.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val specialty: String,
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0,
    val averageTimeSeconds: Float = 0f,
    val lastStudiedAt: Long = System.currentTimeMillis(),
    val weakestSubTopics: String = "[]"  // JSON array
)
