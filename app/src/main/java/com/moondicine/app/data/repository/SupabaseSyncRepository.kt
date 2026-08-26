package com.moondicine.app.data.repository

import android.util.Log
import com.google.gson.Gson
import com.moondicine.app.data.database.entity.AIExplanationEntity
import com.moondicine.app.data.database.entity.AnswerOptionEntity
import com.moondicine.app.data.database.entity.QuestionEntity
import com.moondicine.app.data.remote.SupabaseApi
import com.moondicine.app.data.remote.SupabaseQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSyncRepository @Inject constructor(
    private val supabaseApi: SupabaseApi,
    private val questionRepository: QuestionRepository,
    private val explanationDao: com.moondicine.app.data.database.dao.ExplanationDao
) {
    companion object {
        private const val TAG = "SupabaseSync"
        private const val SYNC_TIMEOUT_MS = 30_000L
    }

    suspend fun syncQuestionBank(): Result<Int> = withContext(Dispatchers.IO) {
        val result = withTimeoutOrNull(SYNC_TIMEOUT_MS) {
            runCatching {
                val questions = supabaseApi.getQuestions()
                val options = supabaseApi.getAnswerOptions()
                val allowedQuestionIds = questions.map { it.id }.toSet()
                val optionsByQuestion = options
                    .filter { it.questionId in allowedQuestionIds }
                    .groupBy { it.questionId }
                var syncedCount = 0

                for (remoteQuestion in questions) {
                    val localQuestion = questionRepository.getQuestionByRemoteId(remoteQuestion.id)
                    val localId = if (localQuestion == null) {
                        questionRepository.insertQuestion(remoteQuestion.toLocal())
                    } else {
                        questionRepository.insertQuestion(remoteQuestion.toLocal(id = localQuestion.id))
                    }

                    for (option in optionsByQuestion[remoteQuestion.id].orEmpty()) {
                        val localOption = questionRepository.getOptionByRemoteId(option.id)
                        questionRepository.insertOption(
                            AnswerOptionEntity(
                                id = localOption?.id ?: 0L,
                                questionId = localId,
                                optionLetter = option.optionLetter,
                                optionText = option.optionText,
                                isCorrect = option.isCorrect,
                                remoteId = option.id
                            )
                        )
                    }
                    syncedCount++
                }

                // Sync AI explanations
                try {
                    val remoteExplanations = supabaseApi.getAiExplanations()
                    val localQuestionMap = questions.associate { remoteQ ->
                        remoteQ.id to questionRepository.getQuestionByRemoteId(remoteQ.id)
                    }
                    for (remoteExplanation in remoteExplanations) {
                        val localQuestion = localQuestionMap[remoteExplanation.questionId]
                            ?: questionRepository.getQuestionByRemoteId(remoteExplanation.questionId)
                        if (localQuestion != null) {
                            val wrongReasoningJson = when (val wr = remoteExplanation.wrongReasoning) {
                                is String -> wr
                                is Map<*, *> -> Gson().toJson(wr)
                                else -> "{}"
                            }
                            val entity = AIExplanationEntity(
                                questionId = localQuestion.id,
                                explanationText = remoteExplanation.explanationText,
                                correctReasoning = remoteExplanation.correctReasoning,
                                wrongReasoning = wrongReasoningJson
                            )
                            explanationDao.insert(entity)
                        }
                    }
                    Log.i(TAG, "Synced ${remoteExplanations.size} AI explanations")
                } catch (e: Exception) {
                    Log.w(TAG, "AI explanations sync failed (non-critical)", e)
                }

                syncedCount
            }
        }

        when {
            result == null -> {
                Log.w(TAG, "Question sync timed out; continuing with offline data")
                Result.failure(IllegalStateException("Supabase sync timed out"))
            }
            result.isFailure -> {
                Log.w(TAG, "Question sync failed; continuing with offline data", result.exceptionOrNull())
                result
            }
            else -> {
                Log.i(TAG, "Synchronized ${result.getOrThrow()} questions")
                result
            }
        }
    }
}

private fun SupabaseQuestion.toLocal(id: Long = 0L): QuestionEntity {
    return QuestionEntity(
        id = id,
        examSource = examSource,
        questionNumber = questionNumber,
        questionText = questionText,
        specialty = specialty,
        subTopic = subTopic,
        difficulty = difficulty.coerceIn(1, 5),
        imageUrl = imageUrl,
        createdAt = createdAt.toEpochMillisOrNow(),
        updatedAt = updatedAt.toEpochMillisOrNow(),
        remoteId = this.id
    )
}

private fun String?.toEpochMillisOrNow(): Long {
    return runCatching { this?.let(Instant::parse)?.toEpochMilli() }.getOrNull()
        ?: System.currentTimeMillis()
}
