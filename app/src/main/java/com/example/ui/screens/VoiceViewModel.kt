package com.example.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.AiService
import com.example.data.local.preferences.SettingsRepository
import com.example.voice.VoiceAssistantManager
import com.example.voice.VoiceEngineState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class VoiceTurn(
    val id: Long = System.nanoTime(),
    val userText: String,
    val assistantText: String,
    val isStreaming: Boolean = false
)

data class VoiceUiState(
    val engine: VoiceEngineState = VoiceEngineState(),
    val selectedLocale: String = "auto",
    val ttsEnabled: Boolean = true,
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val isGenerating: Boolean = false,
    val turns: List<VoiceTurn> = emptyList(),
    val errorMessage: String? = null
)

class VoiceViewModel(
    appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val aiService: AiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private val voiceManager: VoiceAssistantManager = VoiceAssistantManager(
        context = appContext.applicationContext,
        onFinalTranscript = { transcript ->
            submitVoiceQuery(transcript)
        },
        onSpeechError = { err ->
            _uiState.value = _uiState.value.copy(errorMessage = err)
        }
    )

    init {
        viewModelScope.launch {
            voiceManager.state.collectLatest { eng ->
                _uiState.value = _uiState.value.copy(engine = eng)
            }
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                voiceManager.updateTtsParameters(settings.ttsSpeechRate, settings.ttsPitch)
                _uiState.value = _uiState.value.copy(
                    ttsEnabled = settings.ttsEnabled,
                    selectedLocale = settings.voiceLocale,
                    ttsSpeechRate = settings.ttsSpeechRate,
                    ttsPitch = settings.ttsPitch
                )
            }
        }
    }

    fun selectLocale(locale: String) {
        viewModelScope.launch {
            val s = _uiState.value
            _uiState.value = s.copy(selectedLocale = locale)
            settingsRepository.updateVoiceSettings(
                enabled = s.ttsEnabled,
                rate = s.ttsSpeechRate,
                pitch = s.ttsPitch,
                locale = locale
            )
        }
    }

    fun toggleTtsEnabled() {
        val current = _uiState.value
        val next = !current.ttsEnabled
        if (!next) voiceManager.stopSpeaking()
        _uiState.value = current.copy(ttsEnabled = next)
        viewModelScope.launch {
            settingsRepository.updateVoiceSettings(
                enabled = next,
                rate = current.ttsSpeechRate,
                pitch = current.ttsPitch,
                locale = current.selectedLocale
            )
        }
    }

    fun startListening() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
        voiceManager.startListening(_uiState.value.selectedLocale)
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun speakText(text: String) {
        voiceManager.speak(text, _uiState.value.selectedLocale)
    }

    fun submitVoiceQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || _uiState.value.isGenerating) return

        viewModelScope.launch {
            val turnId = System.nanoTime()
            val placeholder = VoiceTurn(
                id = turnId,
                userText = trimmed,
                assistantText = "",
                isStreaming = true
            )
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                errorMessage = null,
                turns = listOf(placeholder) + _uiState.value.turns
            )

            try {
                val builder = StringBuilder()
                val voiceInstruction = """
                    You are AI ASISSTANT Voice Mode.
                    Give clear, conversational answers suitable for spoken playback in either Sinhala (සිංහල) or English depending on the user's question.
                    Never claim you can dial calls, change system volume/brightness, or control Android OS hardware settings.
                """.trimIndent()

                aiService.streamSinglePrompt(
                    prompt = trimmed,
                    systemInstruction = voiceInstruction
                ).collect { chunk ->
                    builder.append(chunk)
                    val updated = builder.toString()
                    _uiState.value = _uiState.value.copy(
                        turns = _uiState.value.turns.map {
                            if (it.id == turnId) it.copy(assistantText = updated) else it
                        }
                    )
                }

                val finalReply = builder.toString().trim()
                if (finalReply.isEmpty()) {
                    throw IllegalStateException("Model returned an empty response.")
                }

                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    turns = _uiState.value.turns.map {
                        if (it.id == turnId) it.copy(assistantText = finalReply, isStreaming = false)
                        else it
                    }
                )
                if (_uiState.value.ttsEnabled) {
                    voiceManager.speak(finalReply, _uiState.value.selectedLocale)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = AiService.formatUserError(e)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = msg,
                    turns = _uiState.value.turns.map {
                        if (it.id == turnId) it.copy(assistantText = msg, isStreaming = false) else it
                    }
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }

    companion object {
        fun provideFactory(
            context: Context,
            settingsRepository: SettingsRepository,
            aiService: AiService
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { VoiceViewModel(context.applicationContext, settingsRepository, aiService) }
        }
    }
}
