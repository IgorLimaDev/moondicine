package com.moondicine.app.data.database.dao

import androidx.room.*
import com.moondicine.app.data.database.entity.AIExplanationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExplanationDao {

    @Upsert
    suspend fun insert(explanation: AIExplanationEntity): Long

    @Query("SELECT * FROM ai_explanations WHERE questionId = :questionId LIMIT 1")
    suspend fun getByQuestionId(questionId: Long): AIExplanationEntity?

    @Query("SELECT * FROM ai_explanations WHERE questionId = :questionId LIMIT 1")
    fun getByQuestionIdFlow(questionId: Long): Flow<AIExplanationEntity?>

    @Query("SELECT COUNT(*) FROM ai_explanations")
    suspend fun getCount(): Int

    @Query("DELETE FROM ai_explanations")
    suspend fun deleteAll()
}
