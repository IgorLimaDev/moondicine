package com.moondicine.app.data.database.dao

import androidx.room.*
import com.moondicine.app.data.database.entity.AnswerOptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerOptionDao {

    @Upsert
    suspend fun insertAll(options: List<AnswerOptionEntity>): List<Long>

    @Upsert
    suspend fun insert(option: AnswerOptionEntity): Long

    @Delete
    suspend fun delete(option: AnswerOptionEntity)

    @Query("SELECT * FROM answer_options WHERE questionId = :questionId ORDER BY optionLetter ASC")
    suspend fun getByQuestionId(questionId: Long): List<AnswerOptionEntity>

    @Query("SELECT * FROM answer_options WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: Long): AnswerOptionEntity?

    @Query("SELECT * FROM answer_options WHERE questionId = :questionId ORDER BY optionLetter ASC")
    fun getByQuestionIdFlow(questionId: Long): Flow<List<AnswerOptionEntity>>

    @Query("SELECT * FROM answer_options WHERE questionId = :questionId AND isCorrect = 1 LIMIT 1")
    suspend fun getCorrectOption(questionId: Long): AnswerOptionEntity?

    @Query("DELETE FROM answer_options WHERE questionId = :questionId")
    suspend fun deleteByQuestionId(questionId: Long)

    @Query("DELETE FROM answer_options")
    suspend fun deleteAll()
}
