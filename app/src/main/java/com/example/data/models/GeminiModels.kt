package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @param:Json(name = "contents") val contents: List<GeminiContent>,
    @param:Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @param:Json(name = "generationConfig") val generationConfig: GeminiGenConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @param:Json(name = "parts") val parts: List<GeminiPart>,
    @param:Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @param:Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    @param:Json(name = "thinkingLevel") val thinkingLevel: String = "HIGH"
)

@JsonClass(generateAdapter = true)
data class GeminiGenConfig(
    @param:Json(name = "temperature") val temperature: Float? = null,
    @param:Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null,
    @param:Json(name = "thinkingConfig") val thinkingConfig: ThinkingConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @param:Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @param:Json(name = "content") val content: GeminiContent? = null,
    @param:Json(name = "finishReason") val finishReason: String? = null
)

