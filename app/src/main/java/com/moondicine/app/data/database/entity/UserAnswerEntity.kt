package com.moondicine.app.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_answers",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AnswerOptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["selectedOptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["questionId"]), Index(value = ["selectedOptionId"])]
)
data class UserAnswerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionId: Long,
    val selectedOptionId: Long,
    val isCorrect: Boolean,
    val timeSpentSeconds: Int = 0,
    val answeredAt: Long = System.currentTimeMillis(),
    val isFlagged: Boolean = false
)
