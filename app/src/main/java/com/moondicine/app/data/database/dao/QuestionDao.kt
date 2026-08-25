package com.moondicine.app.data.database.dao

import androidx.room.*
import com.moondicine.app.data.database.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: QuestionEntity): Long

    @Update
    suspend fun update(question: QuestionEntity)

    @Delete
    suspend fun delete(question: QuestionEntity)

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: Long): QuestionEntity?

    @Query("SELECT * FROM questions WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: Long): QuestionEntity?

    @Query("SELECT * FROM questions WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<QuestionEntity?>

    @Query("SELECT * FROM questions ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions ORDER BY createdAt DESC")
    suspend fun getAll(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE specialty = :specialty ORDER BY createdAt DESC")
    fun getBySpecialtyFlow(specialty: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE specialty = :specialty ORDER BY createdAt DESC")
    suspend fun getBySpecialty(specialty: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE difficulty = :difficulty ORDER BY createdAt DESC")
    suspend fun getByDifficulty(difficulty: Int): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE examSource = :examSource ORDER BY questionNumber ASC")
    suspend fun getByExamSource(examSource: String): List<QuestionEntity>

    @Query("""
        SELECT * FROM questions 
        WHERE id NOT IN (
            SELECT questionId FROM user_answers
        ) 
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun getUnanswered(limit: Int): List<QuestionEntity>

    @Query("""
        SELECT * FROM questions 
        WHERE id NOT IN (
            SELECT questionId FROM user_answers
        ) 
        AND specialty = :specialty
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun getUnansweredBySpecialty(specialty: String, limit: Int): List<QuestionEntity>

    @Query("""
        SELECT * FROM questions
        WHERE id NOT IN (SELECT questionId FROM user_answers)
        AND examSource = :examSource
        ORDER BY questionNumber ASC
    """)
    suspend fun getUnansweredByExamSource(examSource: String): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getCount(): Int

    @Query("SELECT DISTINCT specialty FROM questions ORDER BY specialty")
    fun getAllSpecialtiesFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT specialty FROM questions ORDER BY specialty")
    suspend fun getAllSpecialties(): List<String>

    @Query("SELECT DISTINCT examSource FROM questions ORDER BY examSource")
    suspend fun getAllExamSources(): List<String>

    @Query("DELETE FROM questions")
    suspend fun deleteAll()
}
