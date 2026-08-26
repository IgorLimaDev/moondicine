package com.moondicine.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface SupabaseApi {
    @GET("rest/v1/questions")
    suspend fun getQuestions(
        @Query("select") select: String = "*",
        @Query("order") order: String = "updated_at.desc"
    ): List<SupabaseQuestion>

    @GET("rest/v1/answer_options")
    suspend fun getAnswerOptions(
        @Query("select") select: String = "*",
        @Query("order") order: String = "question_id.asc,option_letter.asc"
    ): List<SupabaseAnswerOption>

    @GET("rest/v1/ai_explanations")
    suspend fun getAiExplanations(
        @Query("select") select: String = "*",
        @Query("order") order: String = "question_id.asc"
    ): List<SupabaseAiExplanation>
}
