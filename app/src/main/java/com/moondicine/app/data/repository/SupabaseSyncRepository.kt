package com.moondicine.app.data.repository

import android.util.Log
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
    private val questionRepository: QuestionRepository
) {
    companion object {
        private const val TAG = "SupabaseSync"
        private const val SYNC_TIMEOUT_MS = 15_000L
    }

    suspend fun syncQuestionBank(): Result<Int> = withContext(Dispatchers.IO) {
        val result = withTimeoutOrNull(SYNC_TIMEOUT_MS) {
            runCatching {
                val questions = supabaseApi.getQuestions()
                val options = supabaseApi.getAnswerOptions()
                val allowedQuestions = questions.filter { it.specialty in MAJOR_SPECIALTIES }
                val allowedQuestionIds = allowedQuestions.map { it.id }.toSet()
                val optionsByQuestion = options
                    .filter { it.questionId in allowedQuestionIds }
                    .groupBy { it.questionId }
                var syncedCount = 0

                for (remoteQuestion in allowedQuestions) {
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

private val MAJOR_SPECIALTIES = setOf(
    "Clínica Médica",
    "Cirurgia Geral",
    "Pediatria",
    "Ginecologia e Obstetrícia",
    "Medicina Preventiva"
)

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
