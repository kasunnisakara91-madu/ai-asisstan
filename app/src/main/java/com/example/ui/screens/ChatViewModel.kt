package com.example.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.AiService
import com.example.data.local.db.ConversationSummary
import com.example.data.local.db.MessageEntity
import com.example.data.local.preferences.AppLanguage
import com.example.data.local.preferences.SettingsRepository
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val activeConversationId: Long? = null,
    val messages: List<MessageEntity> = emptyList(),
    val streamingText: String = "",
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val lastFailedPrompt: String? = null,
    val searchQuery: String = "",
    val conversations: List<ConversationSummary> = emptyList(),
    val activeLanguage: AppLanguage = AppLanguage.AUTO,
    val activeModelName: String = "gemini-3.8-flash"
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val aiService: AiService,
    initialConversationId: Long? = null
) : ViewModel() {

    private val _activeConversationId = MutableStateFlow(initialConversationId)
    private val _streamingText = MutableStateFlow("")
    private val _isGenerating = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _lastFailedPrompt = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    private var activeGenerationJob: Job? = null

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val activeMessagesFlow = _activeConversationId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else chatRepository.observeMessages(id)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val conversationsFlow = _searchQuery.flatMapLatest { query ->
        chatRepository.observeConversations(query.trim())
    }

    val uiState: StateFlow<ChatUiState> = combine(
        _activeConversationId,
        activeMessagesFlow,
        _streamingText,
        _isGenerating,
        _errorMessage,
        _lastFailedPrompt,
        _searchQuery,
        conversationsFlow,
        settingsRepository.settingsFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ChatUiState(
            activeConversationId = values[0] as Long?,
            messages = values[1] as List<MessageEntity>,
            streamingText = values[2] as String,
            isGenerating = values[3] as Boolean,
            errorMessage = values[4] as String?,
            lastFailedPrompt = values[5] as String?,
            searchQuery = values[6] as String,
            conversations = values[7] as List<ConversationSummary>,
            activeLanguage = (values[8] as com.example.data.local.preferences.AppSettings).appLanguage,
            activeModelName = (values[8] as com.example.data.local.preferences.AppSettings).geminiModelName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState(activeConversationId = initialConversationId)
    )

    fun selectConversation(conversationId: Long?) {
        activeGenerationJob?.cancel()
        _isGenerating.value = false
        _streamingText.value = ""
        _errorMessage.value = null
        _lastFailedPrompt.value = null
        _activeConversationId.value = conversationId
    }

    fun startNewChat() {
        selectConversation(null)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.updateAppLanguage(language)
        }
    }

    fun sendMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() || _isGenerating.value) return

        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            _errorMessage.value = null
            _lastFailedPrompt.value = null
            _streamingText.value = ""
            _isGenerating.value = true

            var convId = _activeConversationId.value
            try {
                if (convId == null) {
                    val lang = uiState.value.activeLanguage.code
                    convId = chatRepository.createConversation(prompt, lang)
                    _activeConversationId.value = convId
                }
                val historyBeforePrompt = chatRepository.getMessagesOnce(convId)
                chatRepository.addMessage(convId, role = "user", content = prompt)

                val buffer = StringBuilder()
                aiService.streamChat(
                    history = historyBeforePrompt,
                    userMessage = prompt
                ).collect { chunk ->
                    buffer.append(chunk)
                    _streamingText.value = buffer.toString()
                }

                val finalText = buffer.toString().trim()
                if (finalText.isNotEmpty()) {
                    chatRepository.addMessage(convId, role = "model", content = finalText)
                } else {
                    val errMsg = "Model returned an empty response."
                    chatRepository.addMessage(convId, role = "model", content = errMsg, isError = true)
                    _errorMessage.value = errMsg
                    _lastFailedPrompt.value = prompt
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val formatted = AiService.formatUserError(e)
                Log.e(TAG, "Chat error: $formatted", e)
                _errorMessage.value = formatted
                _lastFailedPrompt.value = prompt
                convId?.let {
                    chatRepository.addMessage(it, role = "model", content = formatted, isError = true)
                }
            } finally {
                _streamingText.value = ""
                _isGenerating.value = false
            }
        }
    }

    fun regenerateLastResponse() {
        val convId = _activeConversationId.value ?: return
        if (_isGenerating.value) return

        viewModelScope.launch {
            val messages = chatRepository.getMessagesOnce(convId)
            val lastUserMsg = messages.lastOrNull { it.role == "user" } ?: return@launch
            val lastMsg = messages.lastOrNull()
            if (lastMsg != null && lastMsg.role == "model") {
                chatRepository.deleteMessageById(lastMsg.id)
            }
            val remaining = chatRepository.getMessagesOnce(convId)
                .dropLastWhile { it.id == lastMsg?.id }
            val historyBefore = remaining.filter { it.id < lastUserMsg.id }

            _errorMessage.value = null
            _lastFailedPrompt.value = null
            _streamingText.value = ""
            _isGenerating.value = true

            try {
                val buffer = StringBuilder()
                aiService.streamChat(
                    history = historyBefore,
                    userMessage = lastUserMsg.content
                ).collect { chunk ->
                    buffer.append(chunk)
                    _streamingText.value = buffer.toString()
                }
                val finalText = buffer.toString().trim()
                if (finalText.isNotEmpty()) {
                    chatRepository.addMessage(convId, role = "model", content = finalText)
                } else {
                    _errorMessage.value = "Model returned an empty response."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val formatted = AiService.formatUserError(e)
                _errorMessage.value = formatted
                _lastFailedPrompt.value = lastUserMsg.content
                chatRepository.addMessage(convId, role = "model", content = formatted, isError = true)
            } finally {
                _streamingText.value = ""
                _isGenerating.value = false
            }
        }
    }

    fun retryLastFailed() {
        val convId = _activeConversationId.value ?: return
        if (_isGenerating.value) return
        viewModelScope.launch {
            val msgs = chatRepository.getMessagesOnce(convId)
            val last = msgs.lastOrNull()
            if (last != null && last.isError) {
                chatRepository.deleteMessageById(last.id)
            }
            val prompt = _lastFailedPrompt.value
                ?: msgs.lastOrNull { it.role == "user" }?.content
                ?: return@launch
            val historyBefore = chatRepository.getMessagesOnce(convId)
                .filter { !it.isError && it.content != prompt }

            _errorMessage.value = null
            _streamingText.value = ""
            _isGenerating.value = true
            try {
                val buffer = StringBuilder()
                aiService.streamChat(history = historyBefore, userMessage = prompt).collect { chunk ->
                    buffer.append(chunk)
                    _streamingText.value = buffer.toString()
                }
                val finalText = buffer.toString().trim()
                if (finalText.isNotEmpty()) {
                    chatRepository.addMessage(convId, role = "model", content = finalText)
                    _lastFailedPrompt.value = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val formatted = AiService.formatUserError(e)
                _errorMessage.value = formatted
                chatRepository.addMessage(convId, role = "model", content = formatted, isError = true)
            } finally {
                _streamingText.value = ""
                _isGenerating.value = false
            }
        }
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            chatRepository.clearAllHistory()
            _activeConversationId.value = null
        }
    }

    companion object {
        private const val TAG = "ChatViewModel"

        fun provideFactory(
            chatRepository: ChatRepository,
            settingsRepository: SettingsRepository,
            aiService: AiService,
            initialConversationId: Long? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ChatViewModel(chatRepository, settingsRepository, aiService, initialConversationId)
            }
        }
    }
}
