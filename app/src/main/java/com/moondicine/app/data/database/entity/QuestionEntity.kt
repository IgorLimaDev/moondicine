package com.moondicine.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "questions", indices = [Index(value = ["remoteId"], unique = true)])
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val examSource: String,          // e.g., "ENAM 2024", "ANCAR 2023"
    val questionNumber: Int,         // Number within the exam
    val questionText: String,        // Full question text
    val specialty: String,           // Primary medical specialty
    val subTopic: String,            // Specific sub-topic
    val difficulty: Int = 3,         // 1-5 scale
    val imageUrl: String? = null,    // Optional image reference
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val remoteId: Long? = null
)
