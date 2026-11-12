package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.AiService
import com.example.ai.ConfigurationRequiredException
import com.example.data.local.preferences.SettingsRepository
import com.example.data.local.preferences.WebSearchProvider
import com.example.data.remote.WebSearchRepository
import com.example.data.remote.WebSearchResultItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WebSearchUiState(
    val query: String = "",
    val activeProvider: WebSearchProvider = WebSearchProvider.WIKIPEDIA_LIVE,
    val results: List<WebSearchResultItem> = emptyList(),
    val aiSummary: String = "",
    val isSearching: Boolean = false,
    val isSummarizing: Boolean = false,
    val errorMessage: String? = null,
    val needsConfiguration: Boolean = false,
    val hasSearched: Boolean = false
)

class WebSearchViewModel(
    private val webSearchRepository: WebSearchRepository,
    private val aiService: AiService,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _results = MutableStateFlow<List<WebSearchResultItem>>(emptyList())
    private val _aiSummary = MutableStateFlow("")
    private val _isSearching = MutableStateFlow(false)
    private val _isSummarizing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _needsConfiguration = MutableStateFlow(false)
    private val _hasSearched = MutableStateFlow(false)

    val uiState: StateFlow<WebSearchUiState> = combine(
        _query,
        settingsRepository.settingsFlow,
        _results,
        _aiSummary,
        _isSearching,
        _isSummarizing,
        _errorMessage,
        _needsConfiguration,
        _hasSearched
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        WebSearchUiState(
            query = values[0] as String,
            activeProvider = (values[1] as com.example.data.local.preferences.AppSettings).webSearchProvider,
            results = values[2] as List<WebSearchResultItem>,
            aiSummary = values[3] as String,
            isSearching = values[4] as Boolean,
            isSummarizing = values[5] as Boolean,
            errorMessage = values[6] as String?,
            needsConfiguration = values[7] as Boolean,
            hasSearched = values[8] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WebSearchUiState()
    )

    fun updateQuery(value: String) {
        _query.value = value
    }

    fun executeSearch() {
        val q = _query.value.trim()
        if (q.isEmpty() || _isSearching.value) return

        viewModelScope.launch {
            _isSearching.value = true
            _isSummarizing.value = false
            _errorMessage.value = null
            _needsConfiguration.value = false
            _aiSummary.value = ""
            _results.value = emptyList()

            try {
                val response = webSearchRepository.search(q)
                _results.value = response.results
                _hasSearched.value = true
                _isSearching.value = false

                if (response.results.isEmpty()) {
                    _aiSummary.value = ""
                    return@launch
                }

                // Ground Gemini strictly in real retrieved search results
                _isSummarizing.value = true
                val sourcesBlock = response.results.mapIndexed { idx, item ->
                    "[${idx + 1}] ${item.title}\nURL: ${item.url}\nSnippet: ${item.snippet}"
                }.joinToString("\n\n")

                val summaryPrompt = """
                    Summarize an answer for the query: "$q"
                    Use ONLY the retrieved search results below and cite source numbers like [1], [2].
                    Never invent URLs or facts not backed by these results.
                    
                    Retrieved Search Results (${response.provider.label}):
                    $sourcesBlock
                """.trimIndent()

                val buffer = StringBuilder()
                aiService.streamSinglePrompt(summaryPrompt).collect { chunk ->
                    buffer.append(chunk)
                    _aiSummary.value = buffer.toString()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ConfigurationRequiredException) {
                _needsConfiguration.value = true
                _errorMessage.value = e.message
            } catch (e: Exception) {
                _errorMessage.value = AiService.formatUserError(e)
            } finally {
                _isSearching.value = false
                _isSummarizing.value = false
            }
        }
    }

    companion object {
        fun provideFactory(
            webSearchRepository: WebSearchRepository,
            aiService: AiService,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { WebSearchViewModel(webSearchRepository, aiService, settingsRepository) }
        }
    }
}
