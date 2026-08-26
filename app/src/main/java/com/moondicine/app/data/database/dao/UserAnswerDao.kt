package com.moondicine.app.data.database.dao

import androidx.room.*
import com.moondicine.app.data.database.entity.UserAnswerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAnswerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(answer: UserAnswerEntity): Long

    @Update
    suspend fun update(answer: UserAnswerEntity)

    @Query("SELECT * FROM user_answers WHERE questionId = :questionId ORDER BY answeredAt DESC LIMIT 1")
    suspend fun getLatestByQuestionId(questionId: Long): UserAnswerEntity?

    @Query("SELECT * FROM user_answers WHERE questionId = :questionId ORDER BY answeredAt DESC")
    fun getByQuestionIdFlow(questionId: Long): Flow<List<UserAnswerEntity>>

    @Query("SELECT * FROM user_answers WHERE isCorrect = 0 ORDER BY answeredAt DESC")
    suspend fun getWrongAnswers(): List<UserAnswerEntity>

    @Query("SELECT * FROM user_answers WHERE isCorrect = 0 ORDER BY answeredAt DESC")
    fun getWrongAnswersFlow(): Flow<List<UserAnswerEntity>>

    @Query("""
        SELECT questionId, COUNT(*) as errorCount 
        FROM user_answers 
        WHERE isCorrect = 0 
        GROUP BY questionId 
        ORDER BY errorCount DESC 
        LIMIT :limit
    """)
    suspend fun getMostMissedQuestionIds(limit: Int): List<QuestionErrorCount>

    @Query("""
        SELECT * FROM user_answers 
        WHERE isFlagged = 1 
        ORDER BY answeredAt DESC
    """)
    fun getFlaggedAnswersFlow(): Flow<List<UserAnswerEntity>>

    @Query("""
        SELECT * FROM user_answers 
        WHERE isFlagged = 1 
        ORDER BY answeredAt DESC
    """)
    suspend fun getFlaggedAnswers(): List<UserAnswerEntity>

    @Query("SELECT COUNT(*) FROM user_answers")
    suspend fun getTotalAnswered(): Int

    @Query("SELECT COUNT(*) FROM user_answers WHERE isCorrect = 1")
    suspend fun getTotalCorrect(): Int

    @Query("SELECT COUNT(*) FROM user_answers WHERE isCorrect = 0")
    suspend fun getTotalWrong(): Int

    @Query("""
        SELECT COUNT(*) FROM user_answers 
        WHERE answeredAt >= :startTime AND answeredAt <= :endTime
    """)
    suspend fun getAnswerCountBetween(startTime: Long, endTime: Long): Int

    @Query("""
        SELECT AVG(timeSpentSeconds) FROM user_answers 
        WHERE answeredAt >= :startTime
    """)
    suspend fun getAverageTimeSince(startTime: Long): Float?

    @Query("DELETE FROM user_answers")
    suspend fun deleteAll()

    @Query("UPDATE user_answers SET isFlagged = :isFlagged WHERE id = (SELECT id FROM user_answers WHERE questionId = :questionId ORDER BY answeredAt DESC LIMIT 1)")
    suspend fun updateFlag(questionId: Long, isFlagged: Boolean)
}

data class QuestionErrorCount(
    val questionId: Long,
    val errorCount: Int
)
