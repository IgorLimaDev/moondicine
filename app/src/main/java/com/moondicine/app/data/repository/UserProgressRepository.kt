package com.moondicine.app.data.repository

import androidx.room.withTransaction
import com.moondicine.app.data.database.AppDatabase
import com.moondicine.app.data.database.dao.*
import com.moondicine.app.data.database.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProgressRepository @Inject constructor(
    private val database: AppDatabase,
    private val userAnswerDao: UserAnswerDao,
    private val noteDao: NoteDao,
    private val explanationDao: ExplanationDao,
    private val statsDao: StatsDao,
    private val userProfileDao: UserProfileDao
) {
    // User Answers
    suspend fun saveAnswer(answer: UserAnswerEntity): Long = userAnswerDao.insert(answer)

    suspend fun updateAnswer(answer: UserAnswerEntity) = userAnswerDao.update(answer)

    suspend fun recordAnswer(answer: UserAnswerEntity, specialty: String): Long = database.withTransaction {
        val answerId = userAnswerDao.insert(answer)
        updateSpecialtyStats(specialty, answer.isCorrect, answer.timeSpentSeconds)
        answerId
    }

    suspend fun getLatestAnswerForQuestion(questionId: Long): UserAnswerEntity? =
        userAnswerDao.getLatestByQuestionId(questionId)

    fun getWrongAnswersFlow(): Flow<List<UserAnswerEntity>> = userAnswerDao.getWrongAnswersFlow()

    suspend fun getWrongAnswers(): List<UserAnswerEntity> = userAnswerDao.getWrongAnswers()

    fun getFlaggedAnswersFlow(): Flow<List<UserAnswerEntity>> =
        userAnswerDao.getFlaggedAnswersFlow()

    suspend fun getFlaggedAnswers(): List<UserAnswerEntity> = userAnswerDao.getFlaggedAnswers()

    suspend fun getMostMissedQuestionIds(limit: Int = 20): List<QuestionErrorCount> =
        userAnswerDao.getMostMissedQuestionIds(limit)

    suspend fun getTotalAnswered(): Int = userAnswerDao.getTotalAnswered()

    suspend fun getTotalCorrect(): Int = userAnswerDao.getTotalCorrect()

    suspend fun getTotalWrong(): Int = userAnswerDao.getTotalWrong()

    suspend fun getAccuracy(): Float {
        val total = getTotalAnswered()
        if (total == 0) return 0f
        return getTotalCorrect().toFloat() / total.toFloat()
    }

    // Notes
    suspend fun saveNote(note: QuestionNoteEntity): Long = noteDao.insert(note)

    suspend fun updateNote(note: QuestionNoteEntity) = noteDao.update(note)

    fun getNotesForQuestionFlow(questionId: Long): Flow<List<QuestionNoteEntity>> =
        noteDao.getByQuestionIdFlow(questionId)

    suspend fun getNotesForQuestion(questionId: Long): List<QuestionNoteEntity> =
        noteDao.getByQuestionId(questionId)

    suspend fun searchNotes(query: String): List<QuestionNoteEntity> = noteDao.search(query)

    suspend fun deleteNote(noteId: Long) = noteDao.deleteById(noteId)

    // AI Explanations
    suspend fun getExplanation(questionId: Long): AIExplanationEntity? =
        explanationDao.getByQuestionId(questionId)

    fun getExplanationFlow(questionId: Long): Flow<AIExplanationEntity?> =
        explanationDao.getByQuestionIdFlow(questionId)

    suspend fun cacheExplanation(explanation: AIExplanationEntity): Long =
        explanationDao.insert(explanation)

    // Stats
    suspend fun updateSpecialtyStats(specialty: String, isCorrect: Boolean, timeSpent: Int) {
        val existing = statsDao.getBySpecialty(specialty)
        if (existing != null) {
            val newTotal = existing.totalAnswered + 1
            val newCorrect = existing.totalCorrect + if (isCorrect) 1 else 0
            val newAvgTime = ((existing.averageTimeSeconds * existing.totalAnswered) + timeSpent) / newTotal
            statsDao.update(
                existing.copy(
                    totalAnswered = newTotal,
                    totalCorrect = newCorrect,
                    averageTimeSeconds = newAvgTime,
                    lastStudiedAt = System.currentTimeMillis()
                )
            )
        } else {
            statsDao.insertOrUpdate(
                UserStatsEntity(
                    specialty = specialty,
                    totalAnswered = 1,
                    totalCorrect = if (isCorrect) 1 else 0,
                    averageTimeSeconds = timeSpent.toFloat()
                )
            )
        }
    }

    fun getAllStatsFlow(): Flow<List<UserStatsEntity>> = statsDao.getAllFlow()

    suspend fun getAllStats(): List<UserStatsEntity> = statsDao.getAll()

    // User Profile
    fun getUserProfileFlow(): Flow<UserProfileEntity?> = userProfileDao.getProfileFlow()

    suspend fun getUserProfile(): UserProfileEntity? = userProfileDao.getProfile()

    suspend fun saveUserProfile(profile: UserProfileEntity) = userProfileDao.insertOrUpdate(profile)

    suspend fun isOnboardingCompleted(): Boolean {
        val profile = userProfileDao.getProfile()
        return profile?.onboardingCompleted == true
    }
}
