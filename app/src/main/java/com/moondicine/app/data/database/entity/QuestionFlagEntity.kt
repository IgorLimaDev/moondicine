package com.moondicine.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "question_flags",
    indices = [Index(value = ["questionId"], unique = true)]
)
data class QuestionFlagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionId: Long,
    val flaggedAt: Long = System.currentTimeMillis()
)
