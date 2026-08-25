package com.moondicine.app.di

import android.content.Context
import androidx.room.Room
import com.moondicine.app.data.database.AppDatabase
import com.moondicine.app.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "moondice_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideQuestionDao(db: AppDatabase): QuestionDao = db.questionDao()

    @Provides
    fun provideAnswerOptionDao(db: AppDatabase): AnswerOptionDao = db.answerOptionDao()

    @Provides
    fun provideUserAnswerDao(db: AppDatabase): UserAnswerDao = db.userAnswerDao()

    @Provides
    fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideExplanationDao(db: AppDatabase): ExplanationDao = db.explanationDao()

    @Provides
    fun provideStatsDao(db: AppDatabase): StatsDao = db.statsDao()

    @Provides
    fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE questions ADD COLUMN remoteId INTEGER")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_questions_remoteId ON questions(remoteId)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE answer_options ADD COLUMN remoteId INTEGER")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_answer_options_remoteId ON answer_options(remoteId)")
        }
    }
}
