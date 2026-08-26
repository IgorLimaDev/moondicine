package com.moondicine.app.data.repository

import com.moondicine.app.data.database.dao.AnswerOptionDao
import com.moondicine.app.data.database.dao.QuestionDao
import com.moondicine.app.data.database.entity.AnswerOptionEntity
import com.moondicine.app.data.database.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val questionDao: QuestionDao,
    private val answerOptionDao: AnswerOptionDao
) {
    // Questions
    fun getAllQuestionsFlow(): Flow<List<QuestionEntity>> = questionDao.getAllFlow()

    suspend fun getAllQuestions(): List<QuestionEntity> = questionDao.getAll()

    suspend fun getQuestionById(id: Long): QuestionEntity? = questionDao.getById(id)

    suspend fun getQuestionByRemoteId(remoteId: Long): QuestionEntity? = questionDao.getByRemoteId(remoteId)

    fun getQuestionByIdFlow(id: Long): Flow<QuestionEntity?> = questionDao.getByIdFlow(id)

    suspend fun getQuestionsBySpecialty(specialty: String): List<QuestionEntity> =
        questionDao.getBySpecialty(specialty)

    fun getQuestionsBySpecialtyFlow(specialty: String): Flow<List<QuestionEntity>> =
        questionDao.getBySpecialtyFlow(specialty)

    suspend fun getAllAvailableQuestions(): List<QuestionEntity> = questionDao.getAll()

    suspend fun getAllBySpecialty(specialty: String): List<QuestionEntity> =
        questionDao.getBySpecialty(specialty)

    suspend fun getUnansweredQuestions(limit: Int): List<QuestionEntity> =
        questionDao.getUnanswered(limit)

    suspend fun getUnansweredBySpecialty(specialty: String, limit: Int): List<QuestionEntity> =
        questionDao.getUnansweredBySpecialty(specialty, limit)

    suspend fun getUnansweredByExamSource(examSource: String, limit: Int): List<QuestionEntity> {
        val questions = questionDao.getUnansweredByExamSource(examSource)
        return if (limit > 0) questions.take(limit) else questions
    }

    suspend fun getQuestionCount(): Int = questionDao.getCount()

    fun getAllSpecialties(): Flow<List<String>> = questionDao.getAllSpecialtiesFlow()

    suspend fun getAllSpecialtiesList(): List<String> = questionDao.getAllSpecialties()

    suspend fun getExamSources(): List<String> = questionDao.getAllExamSources()

    suspend fun getQuestionsByExamSource(examSource: String): List<QuestionEntity> =
        questionDao.getByExamSource(examSource)

    suspend fun insertQuestions(questions: List<QuestionEntity>): List<Long> =
        questionDao.insertAll(questions)

    suspend fun insertQuestion(question: QuestionEntity): Long = questionDao.insert(question)

    // Answer Options
    suspend fun getOptionsForQuestion(questionId: Long): List<AnswerOptionEntity> =
        answerOptionDao.getByQuestionId(questionId)

    suspend fun getOptionByRemoteId(remoteId: Long): AnswerOptionEntity? =
        answerOptionDao.getByRemoteId(remoteId)

    fun getOptionsForQuestionFlow(questionId: Long): Flow<List<AnswerOptionEntity>> =
        answerOptionDao.getByQuestionIdFlow(questionId)

    suspend fun getCorrectOption(questionId: Long): AnswerOptionEntity? =
        answerOptionDao.getCorrectOption(questionId)

    suspend fun insertOptions(options: List<AnswerOptionEntity>): List<Long> =
        answerOptionDao.insertAll(options)

    suspend fun insertOption(option: AnswerOptionEntity): Long = answerOptionDao.insert(option)

    // Combined operations
    suspend fun insertQuestionWithOptions(
        question: QuestionEntity,
        options: List<AnswerOptionEntity>
    ): Long {
        val questionId = questionDao.insert(question)
        val optionsWithQuestionId = options.map { it.copy(questionId = questionId) }
        answerOptionDao.insertAll(optionsWithQuestionId)
        return questionId
    }

    suspend fun insertAllQuestionsWithOptions(
        questions: List<QuestionEntity>,
        optionsByQuestion: Map<Int, List<AnswerOptionEntity>>
    ): List<Long> {
        val questionIds = questionDao.insertAll(questions)
        questionIds.forEachIndexed { index, questionId ->
            optionsByQuestion[index]?.let { options ->
                val optionsWithId = options.map { it.copy(questionId = questionId) }
                answerOptionDao.insertAll(optionsWithId)
            }
        }
        return questionIds
    }

    suspend fun deleteAll() {
        answerOptionDao.deleteAll()
        questionDao.deleteAll()
    }
}
