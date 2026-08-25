package com.moondicine.app.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "answer_options",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["questionId"]), Index(value = ["remoteId"], unique = true)]
)
data class AnswerOptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionId: Long,
    val optionLetter: String,       // "A", "B", "C", "D", "E"
    val optionText: String,
    val isCorrect: Boolean,
    val remoteId: Long? = null
)
