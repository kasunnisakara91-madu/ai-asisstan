package com.example.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.security.KeystoreCryptoManager
import com.example.security.SecretCipher
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_assisstant_settings"
)

enum class AppLanguage(val label: String, val code: String) {
    AUTO("Auto (Sinhala & English)", "AUTO"),
    ENGLISH("English", "EN"),
    SINHALA("සිංහල (Sinhala)", "SI")
}

enum class ImageGenProvider(val label: String) {
    FIREBASE_NANO_BANANA("Firebase AI (Nano Banana 2.1)"),
    OPENAI_COMPATIBLE("OpenAI-Compatible Image API")
}

enum class WebSearchProvider(val label: String, val requiresApiKey: Boolean) {
    WIKIPEDIA_LIVE("Wikipedia Live API (No Key Required)", false),
    GOOGLE_CUSTOM_SEARCH("Google Custom Search JSON API", true),
    SERPER_DEV("Serper.dev Google Search API", true),
    TAVILY_AI("Tavily AI Web Search API", true)
}

data class AppSettings(
    val appLanguage: AppLanguage = AppLanguage.AUTO,
    val themePreset: ThemePreset = ThemePreset.NEON_OBSIDIAN,
    val glowIntensity: Float = 1.0f,
    val ttsEnabled: Boolean = true,
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val voiceLocale: String = "auto",
    val geminiModelName: String = "gemini-3.8-flash",
    val useCustomGeminiKey: Boolean = false,
    val geminiApiKeyMasked: String = "Not configured",
    val hasGeminiApiKey: Boolean = false,
    val imageGenProvider: ImageGenProvider = ImageGenProvider.FIREBASE_NANO_BANANA,
    val imageGenEndpoint: String = "https://api.openai.com/v1/images/generations",
    val imageGenModel: String = "gpt-image-1",
    val imageGenApiKeyMasked: String = "Not configured",
    val hasImageGenApiKey: Boolean = false,
    val webSearchProvider: WebSearchProvider = WebSearchProvider.WIKIPEDIA_LIVE,
    val webSearchEngineId: String = "",
    val webSearchApiKeyMasked: String = "Not configured",
    val hasWebSearchApiKey: Boolean = false,
    val profileName: String = "Navigator",
    val profileBio: String = "Bilingual AI & Code Explorer",
    val assistantPersona: String = "Intelligent, accurate, natural in Sinhala and English"
)

class SettingsRepository(
    private val context: Context,
    private val cipher: SecretCipher = KeystoreCryptoManager()
) {
    private object Keys {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val GLOW_INTENSITY = floatPreferencesKey("glow_intensity")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val TTS_RATE = floatPreferencesKey("tts_rate")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val VOICE_LOCALE = stringPreferencesKey("voice_locale")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val USE_CUSTOM_GEMINI_KEY = booleanPreferencesKey("use_custom_gemini_key")
        val GEMINI_API_KEY_ENC = stringPreferencesKey("gemini_api_key_enc")
        val IMAGE_PROVIDER = stringPreferencesKey("image_provider")
        val IMAGE_ENDPOINT = stringPreferencesKey("image_endpoint")
        val IMAGE_MODEL = stringPreferencesKey("image_model")
        val IMAGE_API_KEY_ENC = stringPreferencesKey("image_api_key_enc")
        val SEARCH_PROVIDER = stringPreferencesKey("search_provider")
        val SEARCH_ENGINE_ID = stringPreferencesKey("search_engine_id")
        val SEARCH_API_KEY_ENC = stringPreferencesKey("search_api_key_enc")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_BIO = stringPreferencesKey("profile_bio")
        val ASSISTANT_PERSONA = stringPreferencesKey("assistant_persona")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val geminiPlain = prefs[Keys.GEMINI_API_KEY_ENC]?.let { cipher.decrypt(it) }.orEmpty()
        val imagePlain = prefs[Keys.IMAGE_API_KEY_ENC]?.let { cipher.decrypt(it) }.orEmpty()
        val searchPlain = prefs[Keys.SEARCH_API_KEY_ENC]?.let { cipher.decrypt(it) }.orEmpty()

        AppSettings(
            appLanguage = enumValueOrDefault(prefs[Keys.APP_LANGUAGE], AppLanguage.AUTO),
            themePreset = enumValueOrDefault(prefs[Keys.THEME_PRESET], ThemePreset.NEON_OBSIDIAN),
            glowIntensity = prefs[Keys.GLOW_INTENSITY] ?: 1.0f,
            ttsEnabled = prefs[Keys.TTS_ENABLED] ?: true,
            ttsSpeechRate = prefs[Keys.TTS_RATE] ?: 1.0f,
            ttsPitch = prefs[Keys.TTS_PITCH] ?: 1.0f,
            voiceLocale = prefs[Keys.VOICE_LOCALE] ?: "auto",
            geminiModelName = prefs[Keys.GEMINI_MODEL] ?: "gemini-3.8-flash",
            useCustomGeminiKey = prefs[Keys.USE_CUSTOM_GEMINI_KEY] ?: false,
            geminiApiKeyMasked = KeystoreCryptoManager.maskSecret(geminiPlain),
            hasGeminiApiKey = geminiPlain.isNotBlank(),
            imageGenProvider = enumValueOrDefault(
                prefs[Keys.IMAGE_PROVIDER],
                ImageGenProvider.FIREBASE_NANO_BANANA
            ),
            imageGenEndpoint = prefs[Keys.IMAGE_ENDPOINT]
                ?: "https://api.openai.com/v1/images/generations",
            imageGenModel = prefs[Keys.IMAGE_MODEL] ?: "gpt-image-1",
            imageGenApiKeyMasked = KeystoreCryptoManager.maskSecret(imagePlain),
            hasImageGenApiKey = imagePlain.isNotBlank(),
            webSearchProvider = enumValueOrDefault(
                prefs[Keys.SEARCH_PROVIDER],
                WebSearchProvider.WIKIPEDIA_LIVE
            ),
            webSearchEngineId = prefs[Keys.SEARCH_ENGINE_ID] ?: "",
            webSearchApiKeyMasked = KeystoreCryptoManager.maskSecret(searchPlain),
            hasWebSearchApiKey = searchPlain.isNotBlank(),
            profileName = prefs[Keys.PROFILE_NAME] ?: "Navigator",
            profileBio = prefs[Keys.PROFILE_BIO] ?: "Bilingual AI & Code Explorer",
            assistantPersona = prefs[Keys.ASSISTANT_PERSONA]
                ?: "Intelligent, accurate, natural in Sinhala and English"
        )
    }

    suspend fun getDecryptedGeminiApiKey(): String {
        val prefs = context.settingsDataStore.data.first()
        return prefs[Keys.GEMINI_API_KEY_ENC]?.let { cipher.decrypt(it) }.orEmpty()
    }

    suspend fun getDecryptedImageGenApiKey(): String {
        val prefs = context.settingsDataStore.data.first()
        return prefs[Keys.IMAGE_API_KEY_ENC]?.let { cipher.decrypt(it) }.orEmpty()
    }

    suspend fun getDecryptedWebSearchApiKey(): String {
        val prefs = context.settingsDataStore.data.first()
        return prefs[Keys.SEARCH_API_KEY_ENC]?.let { cipher.decrypt(it) }.orEmpty()
    }

    suspend fun updateAppLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }

    suspend fun updateTheme(preset: ThemePreset, glowIntensity: Float) {
        context.settingsDataStore.edit {
            it[Keys.THEME_PRESET] = preset.name
            it[Keys.GLOW_INTENSITY] = glowIntensity.coerceIn(0.4f, 1.5f)
        }
    }

    suspend fun updateVoiceSettings(enabled: Boolean, rate: Float, pitch: Float, locale: String) {
        context.settingsDataStore.edit {
            it[Keys.TTS_ENABLED] = enabled
            it[Keys.TTS_RATE] = rate.coerceIn(0.5f, 2.0f)
            it[Keys.TTS_PITCH] = pitch.coerceIn(0.5f, 2.0f)
            it[Keys.VOICE_LOCALE] = locale
        }
    }

    suspend fun saveGeminiConfig(modelName: String, useCustomKey: Boolean, newApiKey: String?) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.GEMINI_MODEL] = modelName
            prefs[Keys.USE_CUSTOM_GEMINI_KEY] = useCustomKey
            if (newApiKey != null) {
                val trimmed = newApiKey.trim()
                if (trimmed.isEmpty()) {
                    prefs.remove(Keys.GEMINI_API_KEY_ENC)
                } else {
                    prefs[Keys.GEMINI_API_KEY_ENC] = cipher.encrypt(trimmed)
                }
            }
        }
    }

    suspend fun clearGeminiApiKey() {
        context.settingsDataStore.edit {
            it.remove(Keys.GEMINI_API_KEY_ENC)
            it[Keys.USE_CUSTOM_GEMINI_KEY] = false
        }
    }

    suspend fun saveImageGenConfig(
        provider: ImageGenProvider,
        endpoint: String,
        model: String,
        newApiKey: String?
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.IMAGE_PROVIDER] = provider.name
            if (endpoint.isNotBlank()) prefs[Keys.IMAGE_ENDPOINT] = endpoint.trim()
            if (model.isNotBlank()) prefs[Keys.IMAGE_MODEL] = model.trim()
            if (newApiKey != null) {
                val trimmed = newApiKey.trim()
                if (trimmed.isEmpty()) {
                    prefs.remove(Keys.IMAGE_API_KEY_ENC)
                } else {
                    prefs[Keys.IMAGE_API_KEY_ENC] = cipher.encrypt(trimmed)
                }
            }
        }
    }

    suspend fun clearImageGenApiKey() {
        context.settingsDataStore.edit { it.remove(Keys.IMAGE_API_KEY_ENC) }
    }

    suspend fun saveWebSearchConfig(
        provider: WebSearchProvider,
        engineId: String,
        newApiKey: String?
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SEARCH_PROVIDER] = provider.name
            prefs[Keys.SEARCH_ENGINE_ID] = engineId.trim()
            if (newApiKey != null) {
                val trimmed = newApiKey.trim()
                if (trimmed.isEmpty()) {
                    prefs.remove(Keys.SEARCH_API_KEY_ENC)
                } else {
                    prefs[Keys.SEARCH_API_KEY_ENC] = cipher.encrypt(trimmed)
                }
            }
        }
    }

    suspend fun clearWebSearchApiKey() {
        context.settingsDataStore.edit { it.remove(Keys.SEARCH_API_KEY_ENC) }
    }

    suspend fun updateProfile(name: String, bio: String, persona: String) {
        context.settingsDataStore.edit {
            it[Keys.PROFILE_NAME] = name.ifBlank { "Navigator" }
            it[Keys.PROFILE_BIO] = bio.ifBlank { "Bilingual AI & Code Explorer" }
            it[Keys.ASSISTANT_PERSONA] = persona.ifBlank {
                "Intelligent, accurate, natural in Sinhala and English"
            }
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String?, default: T): T {
        if (name.isNullOrBlank()) return default
        return enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
