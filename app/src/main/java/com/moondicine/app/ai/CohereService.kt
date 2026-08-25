package com.moondicine.app.ai

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.moondicine.app.ai.models.*
import com.moondicine.app.ai.prompts.PromptTemplates
import com.moondicine.app.data.database.entity.AIExplanationEntity
import com.moondicine.app.data.database.entity.AnswerOptionEntity
import com.moondicine.app.data.database.entity.QuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CohereService @Inject constructor(
    private val cohereApi: CohereApi,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "CohereService"
        private const val MAX_CHUNK_SIZE = 8000  // Characters per chunk for API
    }

    /**
     * Parse questions from extracted PDF text using Cohere AI.
     */
    suspend fun parseQuestionsFromText(
        text: String,
        examSource: String = "Unknown"
    ): Result<List<ParsedQuestion>> = withContext(Dispatchers.IO) {
        try {
            // Split text into chunks if too large
            val chunks = splitIntoChunks(text, MAX_CHUNK_SIZE)
            val allQuestions = mutableListOf<ParsedQuestion>()

            for (chunk in chunks) {
                val request = CohereChatRequest(
                    messages = listOf(
                        ChatMessage(role = "system", content = PromptTemplates.systemPrompt()),
                        ChatMessage(role = "user", content = PromptTemplates.parseQuestionsPrompt(chunk))
                    )
                )

                val response = cohereApi.chat(request)
                val responseText = extractTextFromResponse(response)

                if (responseText != null) {
                    val parsed = parseQuestionResponse(responseText)
                    allQuestions.addAll(parsed)
                }
            }

            Log.d(TAG, "Parsed ${allQuestions.size} questions from text")
            Result.success(allQuestions)
        } catch (e: HttpException) {
            Log.e(TAG, "Cohere parsing request failed: HTTP ${e.code()}", e)
            Result.failure(Exception(formatHttpError("AI parsing", e), e))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse questions", e)
            Result.failure(Exception("A análise pela IA falhou: ${e.message}"))
        }
    }

    /**
     * Generate an explanation for a question's answer.
     */
    suspend fun generateExplanation(
        questionText: String,
        options: Map<String, String>,
        correctAnswer: String,
        userAnswer: String? = null
    ): Result<ExplanationResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CohereChatRequest(
                messages = listOf(
                    ChatMessage(role = "system", content = PromptTemplates.systemPrompt()),
                    ChatMessage(
                        role = "user",
                        content = PromptTemplates.generateExplanationPrompt(
                            questionText, options, correctAnswer, userAnswer
                        )
                    )
                )
            )

            val response = cohereApi.chat(request)
            val responseText = extractTextFromResponse(response)

            if (responseText != null) {
                val explanation = parseExplanationResponse(responseText)
                Result.success(explanation)
            } else {
                Result.failure(Exception("A IA não retornou uma resposta"))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "Cohere explanation request failed: HTTP ${e.code()}", e)
            Result.failure(Exception(formatHttpError("Explanation generation", e), e))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate explanation", e)
            Result.failure(Exception("A geração da explicação falhou: ${e.message}"))
        }
    }

    /**
     * Generate baseline assessment questions.
     */
    suspend fun generateBaselineAssessment(): Result<List<ParsedQuestion>> = withContext(Dispatchers.IO) {
        try {
            val request = CohereChatRequest(
                messages = listOf(
                    ChatMessage(role = "system", content = PromptTemplates.systemPrompt()),
                    ChatMessage(role = "user", content = PromptTemplates.baselineAssessmentPrompt())
                )
            )

            val response = cohereApi.chat(request)
            val responseText = extractTextFromResponse(response)

            if (responseText != null) {
                val questions = parseQuestionResponse(responseText)
                Result.success(questions)
            } else {
                Result.failure(Exception("A IA não retornou uma resposta"))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "Cohere assessment request failed: HTTP ${e.code()}", e)
            Result.failure(Exception(formatHttpError("Assessment generation", e), e))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate baseline assessment", e)
            Result.failure(Exception("A geração da avaliação falhou: ${e.message}"))
        }
    }

    // ===== Helper Methods =====

    private fun extractTextFromResponse(response: CohereChatResponse): String? {
        return response.message?.content?.firstOrNull { it.type == "text" }?.text
    }

    private fun formatHttpError(operation: String, error: HttpException): String {
        val responseBody = error.response()?.errorBody()?.string()?.trim()
        val details = responseBody?.takeIf { it.isNotEmpty() } ?: "No response body"
        return "$operation falhou: HTTP ${error.code()} ${error.message()} - $details"
    }

    private fun parseQuestionResponse(json: String): List<ParsedQuestion> {
        return try {
            // Try to extract JSON from the response (may be wrapped in markdown code blocks)
            val cleanJson = extractJsonFromString(json)
            val type = object : TypeToken<ParsedQuestionResponse>() {}.type
            val response: ParsedQuestionResponse = gson.fromJson(cleanJson, type)
            response.questions
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse question JSON: $json", e)
            emptyList()
        }
    }

    private fun parseExplanationResponse(json: String): ExplanationResponse {
        val cleanJson = extractJsonFromString(json)
        return try {
            gson.fromJson(cleanJson, ExplanationResponse::class.java)
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse explanation JSON", e)
            ExplanationResponse(
                correctReasoning = "Não foi possível interpretar a explicação. Tente novamente.",
                wrongReasoning = emptyMap(),
                highYieldPoints = emptyList(),
                relatedTopics = emptyList()
            )
        }
    }

    private fun extractJsonFromString(text: String): String {
        // Try to find JSON within code blocks
        val codeBlockRegex = Regex("```(?:json)?\\s*\\n?(.*?)\\n?\\s*```", RegexOption.DOT_MATCHES_ALL)
        val match = codeBlockRegex.find(text)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        // Try to find JSON by looking for { or [
        val jsonStart = text.indexOfFirst { it == '{' || it == '[' }
        val jsonEnd = text.indexOfLast { it == '}' || it == ']' }
        if (jsonStart != -1 && jsonEnd > jsonStart) {
            return text.substring(jsonStart, jsonEnd + 1)
        }

        return text
    }

    private fun splitIntoChunks(text: String, maxSize: Int): List<String> {
        if (text.length <= maxSize) return listOf(text)

        val chunks = mutableListOf<String>()
        val paragraphs = text.split("\n\n")
        var currentChunk = StringBuilder()

        for (paragraph in paragraphs) {
            if (currentChunk.length + paragraph.length > maxSize) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk = StringBuilder()
                }
                // If a single paragraph is too large, split it by sentences
                if (paragraph.length > maxSize) {
                    val sentences = paragraph.split(Regex("(?<=[.!?])\\s+"))
                    for (sentence in sentences) {
                        if (currentChunk.length + sentence.length > maxSize) {
                            if (currentChunk.isNotEmpty()) {
                                chunks.add(currentChunk.toString())
                                currentChunk = StringBuilder()
                            }
                        }
                        currentChunk.append(sentence).append(" ")
                    }
                } else {
                    currentChunk.append(paragraph)
                }
            } else {
                currentChunk.append(paragraph).append("\n\n")
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }

        return chunks
    }
}
