package com.moondicine.app.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_explanations",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["questionId"], unique = true)]
)
data class AIExplanationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionId: Long,
    val explanationText: String,
    val correctReasoning: String,
    val wrongReasoning: String,     // JSON map of option letter -> why it's wrong
    val cachedAt: Long = System.currentTimeMillis()
)
