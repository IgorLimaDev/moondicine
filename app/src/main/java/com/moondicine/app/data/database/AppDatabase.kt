package com.moondicine.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.moondicine.app.data.database.dao.*
import com.moondicine.app.data.database.entity.*

@Database(
    entities = [
        QuestionEntity::class,
        AnswerOptionEntity::class,
        UserAnswerEntity::class,
        QuestionNoteEntity::class,
        AIExplanationEntity::class,
        UserStatsEntity::class,
        UserProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun answerOptionDao(): AnswerOptionDao
    abstract fun userAnswerDao(): UserAnswerDao
    abstract fun noteDao(): NoteDao
    abstract fun explanationDao(): ExplanationDao
    abstract fun statsDao(): StatsDao
    abstract fun userProfileDao(): UserProfileDao
}
