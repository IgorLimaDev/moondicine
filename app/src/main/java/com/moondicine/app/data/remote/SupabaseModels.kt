package com.moondicine.app.data.remote

import com.google.gson.annotations.SerializedName

// Public question-bank records returned by Supabase REST.
data class SupabaseQuestion(
    val id: Long,
    @SerializedName("exam_source") val examSource: String,
    @SerializedName("question_number") val questionNumber: Int,
    @SerializedName("question_text") val questionText: String,
    val specialty: String,
    @SerializedName("sub_topic") val subTopic: String,
    val difficulty: Int,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class SupabaseAnswerOption(
    val id: Long,
    @SerializedName("question_id") val questionId: Long,
    @SerializedName("option_letter") val optionLetter: String,
    @SerializedName("option_text") val optionText: String,
    @SerializedName("is_correct") val isCorrect: Boolean
)

data class SupabaseAiExplanation(
    val id: Long,
    @SerializedName("question_id") val questionId: Long,
    @SerializedName("explanation_text") val explanationText: String,
    @SerializedName("correct_reasoning") val correctReasoning: String,
    @SerializedName("wrong_reasoning") val wrongReasoning: Any?,
    @SerializedName("cached_at") val cachedAt: String?
)
