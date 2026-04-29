package com.forma.app.data.remote.api

import com.forma.app.data.remote.dto.ChatRequest
import com.forma.app.data.remote.dto.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiApi {

    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
