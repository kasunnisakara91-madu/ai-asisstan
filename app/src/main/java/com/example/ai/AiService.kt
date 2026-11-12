package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.data.local.db.MessageEntity
import com.example.data.local.preferences.AppLanguage
import com.example.data.local.preferences.ImageGenProvider
import com.example.data.local.preferences.SettingsRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class ConfigurationRequiredException(message: String) : Exception(message)

data class ChatHistoryTurn(
    val role: String,
    val text: String
)

class AiService(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private fun buildSystemInstruction(language: AppLanguage, persona: String): String {
        val langRule = when (language) {
            AppLanguage.AUTO ->
                "Automatically detect the user's language. If the user writes in Sinhala (සිංහල) or Singlish, respond naturally and fluently in proper Sinhala script. If the user writes in English, respond in English."
            AppLanguage.ENGLISH ->
                "Respond clearly in English unless the user explicitly asks for Sinhala translation or bilingual output."
            AppLanguage.SINHALA ->
                "Respond naturally, fluently, and accurately in Sinhala (සිංහල) script. Keep technical terms or code blocks clear."
        }
        return """
            You are AI ASISSTANT, a helpful, accurate, futuristic Android AI companion.
            Persona: $persona.
            Language Policy: $langRule
            Integrity Rules:
            - Never claim to control Android system settings, phone calls, SMS, alarms, or OS internals unless explicitly performed by the user's Android screen.
            - Format structured explanations with clean Markdown and fenced code blocks with language tags (e.g., ```kotlin, ```python).
        """.trimIndent()
    }

    private fun createFirebaseModel(
        modelName: String,
        systemInstructionText: String
    ): GenerativeModel {
        return Firebase.ai.generativeModel(
            modelName = modelName,
            systemInstruction = content { text(systemInstructionText) }
        )
    }

    fun streamChat(
        history: List<MessageEntity>,
        userMessage: String,
        overrideSystemInstruction: String? = null
    ): Flow<String> = flow {
        val settings = settingsRepository.settingsFlow.first()
        val systemInstruction = overrideSystemInstruction
            ?: buildSystemInstruction(settings.appLanguage, settings.assistantPersona)
        val customKey = settingsRepository.getDecryptedGeminiApiKey()
        val firebaseReady = isFirebaseConfigured(context)
        val shouldUseCustomKey =
            (settings.useCustomGeminiKey && customKey.isNotBlank()) ||
                (!firebaseReady && customKey.isNotBlank())

        if (!firebaseReady && customKey.isBlank()) {
            throw ConfigurationRequiredException(
                "No AI backend configured. Configure a Gemini API key in Settings or connect Firebase AI Logic."
            )
        }

        val turns = history
            .filter { !it.isError && it.content.isNotBlank() }
            .takeLast(18)
            .map { ChatHistoryTurn(role = if (it.role == "user") "user" else "model", text = it.content) }

        if (shouldUseCustomKey) {
            streamViaCustomGeminiRest(
                apiKey = customKey,
                modelName = mapRestModelName(settings.geminiModelName),
                systemInstruction = systemInstruction,
                history = turns,
                userPrompt = userMessage
            ).collect { emit(it) }
        } else {
            val model by lazy {
                createFirebaseModel(settings.geminiModelName, systemInstruction)
            }
            val firebaseHistory: List<Content> = turns.map { turn ->
                content(role = turn.role) { text(turn.text) }
            }
            val chat = model.startChat(history = firebaseHistory)
            withTimeout(60_000L) {
                chat.sendMessageStream(userMessage).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) emit(chunkText)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun streamSinglePrompt(
        prompt: String,
        systemInstruction: String? = null,
        preferLiteModel: Boolean = false
    ): Flow<String> = flow {
        val settings = settingsRepository.settingsFlow.first()
        val finalSystem = systemInstruction
            ?: buildSystemInstruction(settings.appLanguage, settings.assistantPersona)
        val chosenModel = if (preferLiteModel) "gemini-3.5-flash-lite" else settings.geminiModelName
        val customKey = settingsRepository.getDecryptedGeminiApiKey()
        val firebaseReady = isFirebaseConfigured(context)
        val shouldUseCustomKey =
            (settings.useCustomGeminiKey && customKey.isNotBlank()) ||
                (!firebaseReady && customKey.isNotBlank())

        if (!firebaseReady && customKey.isBlank()) {
            throw ConfigurationRequiredException(
                "No AI backend configured. Configure a Gemini API key in Settings or connect Firebase AI Logic."
            )
        }

        if (shouldUseCustomKey) {
            streamViaCustomGeminiRest(
                apiKey = customKey,
                modelName = mapRestModelName(chosenModel),
                systemInstruction = finalSystem,
                history = emptyList(),
                userPrompt = prompt
            ).collect { emit(it) }
        } else {
            val model by lazy { createFirebaseModel(chosenModel, finalSystem) }
            withTimeout(60_000L) {
                model.generateContentStream(prompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) emit(chunkText)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateTextOnce(
        prompt: String,
        systemInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        val builder = StringBuilder()
        streamSinglePrompt(prompt, systemInstruction).collect { chunk ->
            builder.append(chunk)
        }
        val text = builder.toString().trim()
        if (text.isEmpty()) {
            throw IllegalStateException("The model returned an empty response.")
        }
        text
    }

    suspend fun generateImage(prompt: String): Bitmap = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settingsFlow.first()
        when (settings.imageGenProvider) {
            ImageGenProvider.FIREBASE_NANO_BANANA -> {
                if (!isFirebaseConfigured(context)) {
                    throw ConfigurationRequiredException(
                        "Firebase AI Logic is not configured in this build. Switch Image Provider to OpenAI-Compatible API in Settings and add an API key."
                    )
                }
                val config = generationConfig {
                    responseModalities = listOf(ResponseModality.IMAGE)
                }
                val imageModel by lazy {
                    Firebase.ai.generativeModel(
                        modelName = "gemini-nano-banana-2.1",
                        generationConfig = config
                    )
                }
                val response = withTimeout(60_000L) {
                    imageModel.generateContent(prompt)
                }
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                imagePart?.image
                    ?: throw IllegalStateException(
                        "Image model did not return an image. Check Firebase Blaze billing or configure a custom image provider in Settings."
                    )
            }

            ImageGenProvider.OPENAI_COMPATIBLE -> {
                val apiKey = settingsRepository.getDecryptedImageGenApiKey()
                if (apiKey.isBlank()) {
                    throw ConfigurationRequiredException(
                        "Image generation API key is missing. Open Settings → Image Generation API Configuration to add your encrypted key."
                    )
                }
                generateViaOpenAiCompatibleEndpoint(
                    endpoint = settings.imageGenEndpoint,
                    model = settings.imageGenModel,
                    apiKey = apiKey,
                    prompt = prompt
                )
            }
        }
    }

    private fun mapRestModelName(model: String): String {
        return when (model) {
            "gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-4-argon" -> model
            else -> "gemini-2.5-flash"
        }
    }

    private fun streamViaCustomGeminiRest(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        history: List<ChatHistoryTurn>,
        userPrompt: String
    ): Flow<String> = callbackFlow {
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/$modelName:streamGenerateContent?alt=sse"
        val contentsArray = JSONArray()
        history.forEach { turn ->
            contentsArray.put(
                JSONObject().apply {
                    put("role", turn.role)
                    put("parts", JSONArray().put(JSONObject().put("text", turn.text)))
                }
            )
        }
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
            }
        )

        val payload = JSONObject().apply {
            put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            )
            put("contents", contentsArray)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", apiKey)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val call = httpClient.newCall(request)
        try {
            withTimeout(60_000L) {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string().orEmpty()
                        val parsed = runCatching {
                            JSONObject(errBody).optJSONObject("error")?.optString("message")
                        }.getOrNull()
                        throw IllegalStateException(
                            parsed?.takeIf { it.isNotBlank() }
                                ?: "Gemini API request failed (HTTP ${response.code})."
                        )
                    }
                    val stream = response.body?.byteStream()
                        ?: throw IllegalStateException("Empty response stream.")
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val trimmed = line!!.trim()
                            if (trimmed.startsWith("data:")) {
                                val jsonText = trimmed.removePrefix("data:").trim()
                                if (jsonText.isNotEmpty() && jsonText != "[DONE]") {
                                    val json = JSONObject(jsonText)
                                    val candidates = json.optJSONArray("candidates")
                                    val parts = candidates?.optJSONObject(0)
                                        ?.optJSONObject("content")
                                        ?.optJSONArray("parts")
                                    if (parts != null) {
                                        for (i in 0 until parts.length()) {
                                            val partText = parts.optJSONObject(i)?.optString("text").orEmpty()
                                            if (partText.isNotEmpty()) {
                                                trySend(partText)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            close()
        } catch (e: CancellationException) {
            call.cancel()
            throw e
        } catch (e: Exception) {
            close(e)
        }
        awaitClose { call.cancel() }
    }

    private suspend fun generateViaOpenAiCompatibleEndpoint(
        endpoint: String,
        model: String,
        apiKey: String,
        prompt: String
    ): Bitmap = withTimeout(60_000L) {
        val bodyJson = JSONObject().apply {
            put("model", model)
            put("prompt", prompt)
            put("n", 1)
            put("size", "1024x1024")
        }
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = runCatching {
                    JSONObject(raw).optJSONObject("error")?.optString("message")
                }.getOrNull()
                throw IllegalStateException(
                    msg?.takeIf { it.isNotBlank() }
                        ?: "Image API error (HTTP ${response.code}). Verify endpoint, model, and API key in Settings."
                )
            }
            val dataItem = JSONObject(raw).optJSONArray("data")?.optJSONObject(0)
                ?: throw IllegalStateException("Image API returned no image payload.")
            val b64 = dataItem.optString("b64_json")
            if (b64.isNotBlank()) {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                return@use BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw IllegalStateException("Failed to decode generated image bytes.")
            }
            val imageUrl = dataItem.optString("url")
            if (imageUrl.isNotBlank()) {
                val imgReq = Request.Builder().url(imageUrl).get().build()
                httpClient.newCall(imgReq).execute().use { imgRes ->
                    if (!imgRes.isSuccessful) {
                        throw IllegalStateException("Failed to download generated image (HTTP ${imgRes.code}).")
                    }
                    val bytes = imgRes.body?.bytes()
                        ?: throw IllegalStateException("Downloaded image payload was empty.")
                    return@use BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: throw IllegalStateException("Failed to decode downloaded image.")
                }
            }
            throw IllegalStateException("Image API response did not include b64_json or url.")
        }
    }

    companion object {
        private const val TAG = "AiService"

        fun formatUserError(error: Throwable): String {
            Log.e(TAG, "AI operation error", error)
            return when (error) {
                is TimeoutCancellationException ->
                    "Request timed out after 60 seconds. Check your connection and try again."
                is QuotaExceededException ->
                    "AI quota exceeded or billing plan limit reached. Try again later or configure a custom API key in Settings."
                is ConfigurationRequiredException -> error.message ?: "Configuration required in Settings."
                else -> error.localizedMessage ?: "An unexpected AI service error occurred."
            }
        }
    }
}
