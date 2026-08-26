package com.moondicine.app.data.database.dao

import androidx.room.*
import com.moondicine.app.data.database.entity.QuestionFlagEntity

@Dao
interface QuestionFlagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flag: QuestionFlagEntity): Long

    @Query("DELETE FROM question_flags WHERE questionId = :questionId")
    suspend fun deleteByQuestionId(questionId: Long)

    @Query("SELECT * FROM question_flags ORDER BY flaggedAt DESC")
    suspend fun getAll(): List<QuestionFlagEntity>

    @Query("SELECT * FROM question_flags WHERE questionId = :questionId LIMIT 1")
    suspend fun getByQuestionId(questionId: Long): QuestionFlagEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM question_flags WHERE questionId = :questionId)")
    suspend fun isQuestionFlagged(questionId: Long): Boolean
}
