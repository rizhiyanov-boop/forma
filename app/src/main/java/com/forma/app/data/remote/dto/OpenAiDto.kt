package com.forma.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Минимальный контракт Chat Completions API с поддержкой Structured Outputs
 * (response_format = json_schema) и reasoning моделей (o-series).
 *
 * temperature — null для reasoning моделей (они его не поддерживают).
 * reasoningEffort — только для reasoning моделей: "low" | "medium" | "high".
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double? = null,
    @SerialName("reasoning_effort")
    val reasoningEffort: String? = null,
    @SerialName("response_format")
    val responseFormat: ResponseFormat? = null
)

@Serializable
data class ChatMessage(
    val role: String,       // "system" | "user" | "assistant"
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String,                  // "json_schema"
    @SerialName("json_schema")
    val jsonSchema: JsonSchemaWrapper
)

@Serializable
data class JsonSchemaWrapper(
    val name: String,
    val strict: Boolean = true,
    val schema: JsonElement
)

@Serializable
data class ChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<Choice> = emptyList()
)

@Serializable
data class Choice(
    val index: Int,
    val message: ChatMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class OpenAiError(
    val error: OpenAiErrorBody
)

@Serializable
data class OpenAiErrorBody(
    val message: String,
    val type: String? = null,
    val code: String? = null
)
