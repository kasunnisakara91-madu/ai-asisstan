package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.preferences.AppLanguage
import com.example.data.local.preferences.AppSettings
import com.example.data.local.preferences.ImageGenProvider
import com.example.data.local.preferences.SettingsRepository
import com.example.data.local.preferences.WebSearchProvider
import com.example.data.repository.ChatRepository
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val conversationCount: Int = 0,
    val messageCount: Int = 0,
    val toastMessage: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _toastMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        chatRepository.observeConversationCount(),
        chatRepository.observeMessageCount(),
        _toastMessage
    ) { settings, convCount, msgCount, toast ->
        SettingsUiState(
            settings = settings,
            conversationCount = convCount,
            messageCount = msgCount,
            toastMessage = toast
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun clearToast() {
        _toastMessage.value = null
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.updateAppLanguage(language)
            _toastMessage.value = "Language set to ${language.label}"
        }
    }

    fun updateTheme(preset: ThemePreset, glowIntensity: Float) {
        viewModelScope.launch {
            settingsRepository.updateTheme(preset, glowIntensity)
        }
    }

    fun updateVoiceSettings(enabled: Boolean, rate: Float, pitch: Float, locale: String) {
        viewModelScope.launch {
            settingsRepository.updateVoiceSettings(enabled, rate, pitch, locale)
        }
    }

    fun saveGeminiConfig(modelName: String, useCustomKey: Boolean, newKey: String?) {
        viewModelScope.launch {
            settingsRepository.saveGeminiConfig(modelName, useCustomKey, newKey)
            _toastMessage.value = "Gemini settings saved securely via Android Keystore"
        }
    }

    fun clearGeminiKey() {
        viewModelScope.launch {
            settingsRepository.clearGeminiApiKey()
            _toastMessage.value = "Stored Gemini API key removed"
        }
    }

    fun saveImageGenConfig(
        provider: ImageGenProvider,
        endpoint: String,
        model: String,
        newKey: String?
    ) {
        viewModelScope.launch {
            settingsRepository.saveImageGenConfig(provider, endpoint, model, newKey)
            _toastMessage.value = "Image generator settings encrypted & saved"
        }
    }

    fun clearImageGenKey() {
        viewModelScope.launch {
            settingsRepository.clearImageGenApiKey()
            _toastMessage.value = "Stored Image API key removed"
        }
    }

    fun saveWebSearchConfig(
        provider: WebSearchProvider,
        engineId: String,
        newKey: String?
    ) {
        viewModelScope.launch {
            settingsRepository.saveWebSearchConfig(provider, engineId, newKey)
            _toastMessage.value = "Web search settings encrypted & saved"
        }
    }

    fun clearWebSearchKey() {
        viewModelScope.launch {
            settingsRepository.clearWebSearchApiKey()
            _toastMessage.value = "Stored Search API key removed"
        }
    }

    fun clearAllChatHistory() {
        viewModelScope.launch {
            chatRepository.clearAllHistory()
            _toastMessage.value = "All conversation history deleted"
        }
    }

    fun updateProfile(name: String, bio: String, persona: String) {
        viewModelScope.launch {
            settingsRepository.updateProfile(name, bio, persona)
            _toastMessage.value = "Profile updated"
        }
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            chatRepository: ChatRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(settingsRepository, chatRepository) }
        }
    }
}
