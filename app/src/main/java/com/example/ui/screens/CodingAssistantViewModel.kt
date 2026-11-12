package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.AiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CodingLanguage(val displayName: String, val tag: String) {
    KOTLIN("Kotlin", "kotlin"),
    PYTHON("Python", "python"),
    JAVASCRIPT("JavaScript", "javascript"),
    JAVA("Java", "java"),
    HTML("HTML", "html"),
    CSS("CSS", "css"),
    CPP("C++", "cpp"),
    PHP("PHP", "php"),
    JSON("JSON", "json")
}

enum class CodingTask(val label: String) {
    GENERATE("Generate"),
    EXPLAIN("Explain"),
    DEBUG("Debug"),
    FIX("Fix Errors"),
    FORMAT("Format")
}

data class CodingUiState(
    val selectedLanguage: CodingLanguage = CodingLanguage.KOTLIN,
    val selectedTask: CodingTask = CodingTask.GENERATE,
    val promptInput: String = "",
    val sourceCodeInput: String = "",
    val outputMarkdown: String = "",
    val isGenerating: Boolean = false,
    val errorMessage: String? = null
)

class CodingAssistantViewModel(
    private val aiService: AiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CodingUiState())
    val uiState: StateFlow<CodingUiState> = _uiState.asStateFlow()

    fun selectLanguage(language: CodingLanguage) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    fun selectTask(task: CodingTask) {
        _uiState.value = _uiState.value.copy(selectedTask = task)
    }

    fun updatePrompt(value: String) {
        _uiState.value = _uiState.value.copy(promptInput = value)
    }

    fun updateSourceCode(value: String) {
        _uiState.value = _uiState.value.copy(sourceCodeInput = value)
    }

    fun formatLocalInputIfPossible() {
        val state = _uiState.value
        if (state.selectedLanguage == CodingLanguage.JSON && state.sourceCodeInput.isNotBlank()) {
            val formatted = runCatching {
                val trimmed = state.sourceCodeInput.trim()
                if (trimmed.startsWith("[")) {
                    org.json.JSONArray(trimmed).toString(2)
                } else {
                    org.json.JSONObject(trimmed).toString(2)
                }
            }.getOrNull()
            if (formatted != null) {
                _uiState.value = state.copy(
                    sourceCodeInput = formatted,
                    outputMarkdown = "```json\n$formatted\n```",
                    errorMessage = null
                )
                return
            }
        }
        runCodingTask()
    }

    fun runCodingTask() {
        val state = _uiState.value
        val prompt = state.promptInput.trim()
        val code = state.sourceCodeInput.trim()
        if (prompt.isEmpty() && code.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Enter a coding instruction or paste code first.")
            return
        }
        if (state.isGenerating) return

        val lang = state.selectedLanguage
        val composedPrompt = buildString {
            appendLine("Language: ${lang.displayName}")
            appendLine("Task: ${state.selectedTask.label}")
            if (prompt.isNotEmpty()) appendLine("Instructions / Error details:\n$prompt")
            if (code.isNotEmpty()) appendLine("\nSource Code:\n```${lang.tag}\n$code\n```")
        }

        val systemInstruction = """
            You are an expert senior software engineer and polyglot code mentor inside AI ASISSTANT.
            Always enclose code inside fenced Markdown blocks with the exact language tag ```${lang.tag}.
            For Generate/Fix/Format tasks, deliver clean, idiomatic, production-ready code with concise explanation.
            For Debug/Explain tasks, pinpoint root causes clearly and show corrected code.
        """.trimIndent()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                errorMessage = null,
                outputMarkdown = ""
            )
            try {
                val buffer = StringBuilder()
                aiService.streamSinglePrompt(
                    prompt = composedPrompt,
                    systemInstruction = systemInstruction
                ).collect { chunk ->
                    buffer.append(chunk)
                    _uiState.value = _uiState.value.copy(outputMarkdown = buffer.toString())
                }
                if (buffer.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        errorMessage = "Model returned an empty response."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isGenerating = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = AiService.formatUserError(e)
                )
            }
        }
    }

    companion object {
        fun provideFactory(aiService: AiService): ViewModelProvider.Factory = viewModelFactory {
            initializer { CodingAssistantViewModel(aiService) }
        }
    }
}
