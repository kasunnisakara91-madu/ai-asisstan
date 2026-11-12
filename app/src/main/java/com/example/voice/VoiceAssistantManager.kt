package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceEngineState(
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val rmsLevel: Float = 0f,
    val partialTranscript: String = "",
    val isRecognitionAvailable: Boolean = true,
    val ttsReady: Boolean = false,
    val sinhalaTtsSupported: Boolean = false
)

class VoiceAssistantManager(
    private val context: Context,
    private val onFinalTranscript: (String) -> Unit,
    private val onSpeechError: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private val _state = MutableStateFlow(
        VoiceEngineState(isRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context))
    )
    val state: StateFlow<VoiceEngineState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var speechRate: Float = 1.0f
    private var speechPitch: Float = 1.0f

    private val sinhalaLocale: Locale = Locale.Builder().setLanguage("si").setRegion("LK").build()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val tts = textToSpeech ?: return
            val siResult = tts.isLanguageAvailable(sinhalaLocale)
            val sinhalaSupported = siResult >= TextToSpeech.LANG_AVAILABLE
            tts.language = Locale.US
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _state.value = _state.value.copy(isSpeaking = true)
                }

                override fun onDone(utteranceId: String?) {
                    _state.value = _state.value.copy(isSpeaking = false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _state.value = _state.value.copy(isSpeaking = false)
                }
            })
            _state.value = _state.value.copy(
                ttsReady = true,
                sinhalaTtsSupported = sinhalaSupported
            )
        } else {
            Log.e(TAG, "TextToSpeech init failed with status $status")
            _state.value = _state.value.copy(ttsReady = false)
        }
    }

    fun updateTtsParameters(rate: Float, pitch: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        speechPitch = pitch.coerceIn(0.5f, 2.0f)
        textToSpeech?.setSpeechRate(speechRate)
        textToSpeech?.setPitch(speechPitch)
    }

    fun startListening(localeTag: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = _state.value.copy(isRecognitionAvailable = false, isListening = false)
            onSpeechError(
                "On-device Speech Recognition service is not installed on this device/emulator. You can type a question below instead."
            )
            return
        }
        stopSpeaking()
        destroyRecognizer()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = _state.value.copy(
                    isListening = true,
                    partialTranscript = "",
                    rmsLevel = 0.1f
                )
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                _state.value = _state.value.copy(rmsLevel = normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _state.value = _state.value.copy(isListening = false, rmsLevel = 0f)
            }

            override fun onError(error: Int) {
                _state.value = _state.value.copy(isListening = false, rmsLevel = 0f)
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network unavailable for speech recognition."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try speaking again."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy. Try again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                    else -> "Speech recognition unavailable (code $error)."
                }
                onSpeechError(msg)
            }

            override fun onResults(results: Bundle?) {
                _state.value = _state.value.copy(isListening = false, rmsLevel = 0f)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull()?.trim().orEmpty()
                if (spoken.isNotEmpty()) {
                    _state.value = _state.value.copy(partialTranscript = spoken)
                    onFinalTranscript(spoken)
                } else {
                    onSpeechError("No transcript recognized.")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    _state.value = _state.value.copy(partialTranscript = partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            val resolvedLocale = when (localeTag) {
                "si-LK" -> "si-LK"
                "en-US" -> "en-US"
                else -> Locale.getDefault().toLanguageTag()
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, resolvedLocale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, resolvedLocale)
        }
        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _state.value = _state.value.copy(isListening = false, rmsLevel = 0f)
    }

    fun speak(text: String, preferredLocale: String) {
        val tts = textToSpeech ?: return
        if (!_state.value.ttsReady || text.isBlank()) return

        val containsSinhala = text.any { it in '\u0D80'..'\u0DFF' }
        val targetLocale = when {
            containsSinhala || preferredLocale == "si-LK" -> sinhalaLocale
            else -> Locale.US
        }

        val availability = tts.isLanguageAvailable(targetLocale)
        if (availability >= TextToSpeech.LANG_AVAILABLE) {
            tts.language = targetLocale
        } else {
            tts.language = Locale.US
        }

        val cleanText = text
            .replace(Regex("```[\\s\\S]*?```"), " code block ")
            .replace(Regex("[*#>`_~]"), "")
            .take(3500)

        tts.setSpeechRate(speechRate)
        tts.setPitch(speechPitch)
        tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "ai_voice_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _state.value = _state.value.copy(isSpeaking = false)
    }

    private fun destroyRecognizer() {
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
    }

    fun release() {
        destroyRecognizer()
        runCatching {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        textToSpeech = null
    }

    companion object {
        private const val TAG = "VoiceAssistantMgr"
    }
}
