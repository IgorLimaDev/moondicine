package com.moondicine.app.ai.models

import com.google.gson.annotations.SerializedName

// ===== Request Models =====

data class CohereChatRequest(
    val model: String = "command-r7b-12-2024",
    val stream: Boolean = false,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.3f,
    @SerializedName("max_tokens")
    val maxTokens: Int = 2000
)

data class ChatMessage(
    val role: String,
    val content: String
)

// ===== Response Models =====

data class CohereChatResponse(
    val message: CohereMessage?,
    val finish_reason: String? = null
)

data class CohereMessage(
    val role: String?,
    val content: List<CohereContent>?
)

data class CohereContent(
    val type: String?,
    val text: String?
)

// ===== Parsed Question Models =====

data class ParsedQuestionResponse(
    val questions: List<ParsedQuestion>
)

data class ParsedQuestion(
    val questionNumber: Int,
    val questionText: String,
    val options: List<ParsedOption>,
    val correctAnswer: String?,  // Letter like "A", "B", etc.
    val specialty: String,
    val subTopic: String,
    val difficulty: Int
)

data class ParsedOption(
    val letter: String,
    val text: String
)

// ===== Explanation Models =====

data class ExplanationResponse(
    val correctReasoning: String,
    val wrongReasoning: Map<String, String>,  // Letter -> reasoning
    val highYieldPoints: List<String>,
    val relatedTopics: List<String>
)
