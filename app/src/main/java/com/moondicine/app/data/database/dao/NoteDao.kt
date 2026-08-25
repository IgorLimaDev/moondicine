package com.moondicine.app.data.database.dao

import androidx.room.*
import com.moondicine.app.data.database.entity.QuestionNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: QuestionNoteEntity): Long

    @Update
    suspend fun update(note: QuestionNoteEntity)

    @Delete
    suspend fun delete(note: QuestionNoteEntity)

    @Query("SELECT * FROM question_notes WHERE questionId = :questionId ORDER BY createdAt DESC")
    fun getByQuestionIdFlow(questionId: Long): Flow<List<QuestionNoteEntity>>

    @Query("SELECT * FROM question_notes WHERE questionId = :questionId ORDER BY createdAt DESC")
    suspend fun getByQuestionId(questionId: Long): List<QuestionNoteEntity>

    @Query("SELECT * FROM question_notes WHERE noteText LIKE '%' || :searchQuery || '%' ORDER BY createdAt DESC")
    suspend fun search(searchQuery: String): List<QuestionNoteEntity>

    @Query("DELETE FROM question_notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)

    @Query("DELETE FROM question_notes WHERE questionId = :questionId")
    suspend fun deleteByQuestionId(questionId: Long)
}
