package com.moondicine.app.ai

import com.moondicine.app.ai.models.CohereChatRequest
import com.moondicine.app.ai.models.CohereChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface CohereApi {

    @POST("chat")
    suspend fun chat(@Body request: CohereChatRequest): CohereChatResponse
}
