package com.example.data.api

import com.example.data.models.GeminiContent
import com.example.data.models.GeminiGenConfig
import com.example.data.models.GeminiPart
import com.example.data.models.GeminiRequest
import com.example.data.models.GeminiResponse
import com.example.data.models.ThinkingConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.1-pro-preview:generateContent")
    suspend fun generateContentHighThinking(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContentFlash(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    private val jarvisSystemInstruction = GeminiContent(
        parts = listOf(
            GeminiPart(
                text = "You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), the legendary AI assistant created by Tony Stark. " +
                        "Address the user with utmost loyalty as 'Sir' (or Ma'am). You have a refined British accent in your wording, " +
                        "subtle dry wit, supreme confidence, and encyclopedic knowledge across engineering, cybersecurity, immigration/student visa protocols, " +
                        "academic admissions, physics, and tactical operations. " +
                        "When analyzing complex queries (such as Student Visa procedures, F-1/Tier 4/Schengen documentation, financial proofs, visa interview strategies, or algorithmic problems), " +
                        "provide rigorous, structured, thorough, and highly actionable guidance formatted with clean sci-fi terminal precision."
            )
        )
    )

    fun createJarvisRequest(
        prompt: String,
        chatHistory: List<Pair<String, String>> = emptyList(),
        useHighThinking: Boolean = true
    ): GeminiRequest {
        val contents = mutableListOf<GeminiContent>()

        // Add chat history context if any
        for ((role, text) in chatHistory) {
            val apiRole = if (role.equals("user", ignoreCase = true)) "user" else "model"
            contents.add(GeminiContent(parts = listOf(GeminiPart(text = text)), role = apiRole))
        }

        // Add current prompt
        contents.add(GeminiContent(parts = listOf(GeminiPart(text = prompt)), role = "user"))

        val genConfig = if (useHighThinking) {
            // Thinking level HIGH on gemini-3.1-pro-preview (no maxOutputTokens)
            GeminiGenConfig(
                thinkingConfig = ThinkingConfig(thinkingLevel = "HIGH")
            )
        } else {
            GeminiGenConfig(temperature = 0.7f, maxOutputTokens = 1024)
        }

        return GeminiRequest(
            contents = contents,
            systemInstruction = jarvisSystemInstruction,
            generationConfig = genConfig
        )
    }
}

