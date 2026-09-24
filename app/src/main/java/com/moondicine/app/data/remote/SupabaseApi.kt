package com.moondicine.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface SupabaseApi {
    @GET("rest/v1/questions")
    suspend fun getQuestions(
        @Query("select") select: String = "*",
        @Query("order") order: String = "updated_at.desc",
        @Query("limit") limit: Int = 1000,
        @Query("offset") offset: Int = 0
    ): List<SupabaseQuestion>

    @GET("rest/v1/answer_options")
    suspend fun getAnswerOptions(
        @Query("select") select: String = "*",
        @Query("order") order: String = "question_id.asc,option_letter.asc",
        @Query("limit") limit: Int = 1000,
        @Query("offset") offset: Int = 0
    ): List<SupabaseAnswerOption>

    @GET("rest/v1/ai_explanations")
    suspend fun getAiExplanations(
        @Query("select") select: String = "*",
        @Query("order") order: String = "question_id.asc",
        @Query("limit") limit: Int = 1000,
        @Query("offset") offset: Int = 0
    ): List<SupabaseAiExplanation>
}
