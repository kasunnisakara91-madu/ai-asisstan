package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.AiService
import com.example.data.pdf.LoadedPdfDocument
import com.example.data.pdf.PdfProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PdfAiAction {
    SUMMARIZE,
    TRANSLATE_SINHALA,
    TRANSLATE_ENGLISH,
    ASK_QUESTION
}

data class PdfReaderUiState(
    val document: LoadedPdfDocument? = null,
    val currentPageIndex: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val showPagePreview: Boolean = true,
    val consentGrantedForDoc: Boolean = false,
    val questionInput: String = "",
    val aiResponse: String = "",
    val isLoadingPdf: Boolean = false,
    val isGeneratingAi: Boolean = false,
    val errorMessage: String? = null
)

class PdfReaderViewModel(
    private val pdfProcessor: PdfProcessor,
    private val aiService: AiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfReaderUiState())
    val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()

    fun loadPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = PdfReaderUiState(isLoadingPdf = true)
            try {
                val doc = pdfProcessor.loadPdfText(uri)
                val bitmap = pdfProcessor.renderPageBitmap(uri, 0)
                _uiState.value = PdfReaderUiState(
                    document = doc,
                    currentPageIndex = 0,
                    currentPageBitmap = bitmap,
                    showPagePreview = bitmap != null,
                    consentGrantedForDoc = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = PdfReaderUiState(
                    errorMessage = e.localizedMessage ?: "Failed to read PDF file."
                )
            }
        }
    }

    fun goToPage(index: Int) {
        val doc = _uiState.value.document ?: return
        val clamped = index.coerceIn(0, (doc.pageCount - 1).coerceAtLeast(0))
        viewModelScope.launch {
            val bmp = pdfProcessor.renderPageBitmap(doc.uri, clamped)
            _uiState.value = _uiState.value.copy(
                currentPageIndex = clamped,
                currentPageBitmap = bmp
            )
        }
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(showPagePreview = !_uiState.value.showPagePreview)
    }

    fun updateQuestion(text: String) {
        _uiState.value = _uiState.value.copy(questionInput = text)
    }

    fun grantAiConsent() {
        _uiState.value = _uiState.value.copy(consentGrantedForDoc = true, errorMessage = null)
    }

    fun runPdfAiAction(action: PdfAiAction) {
        val state = _uiState.value
        val doc = state.document ?: return
        if (!state.consentGrantedForDoc) {
            _uiState.value = state.copy(
                errorMessage = "Please confirm on-screen document processing consent before sending extracted text to AI."
            )
            return
        }
        if (state.isGeneratingAi) return

        val currentPageText = doc.pageTexts.getOrNull(state.currentPageIndex).orEmpty()
        val combinedExcerpt = doc.pageTexts.joinToString("\n\n") { it }
            .take(18_000)

        val prompt = when (action) {
            PdfAiAction.SUMMARIZE -> """
                Summarize this PDF document ("${doc.fileName}", ${doc.pageCount} pages).
                Provide bullet-pointed key takeaways and major sections.
                
                Extracted text:
                $combinedExcerpt
            """.trimIndent()

            PdfAiAction.TRANSLATE_SINHALA -> """
                Translate the following extracted PDF page text (Page ${state.currentPageIndex + 1}) naturally into Sinhala (සිංහල):
                
                $currentPageText
            """.trimIndent()

            PdfAiAction.TRANSLATE_ENGLISH -> """
                Translate the following extracted PDF page text (Page ${state.currentPageIndex + 1}) clearly into English:
                
                $currentPageText
            """.trimIndent()

            PdfAiAction.ASK_QUESTION -> {
                val q = state.questionInput.trim()
                if (q.isEmpty()) {
                    _uiState.value = state.copy(errorMessage = "Enter a question about the PDF first.")
                    return
                }
                """
                Answer the user's question accurately using ONLY the extracted PDF text below.
                If the answer is not present in the document, state that clearly.
                
                Question: $q
                
                Current Page (${state.currentPageIndex + 1}) & Document Excerpt:
                $combinedExcerpt
                """.trimIndent()
            }
        }

        if (currentPageText.isBlank() && combinedExcerpt.isBlank()) {
            _uiState.value = state.copy(
                errorMessage = "No selectable text was found in this PDF (it may contain scanned images only)."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGeneratingAi = true,
                errorMessage = null,
                aiResponse = ""
            )
            try {
                val buffer = StringBuilder()
                aiService.streamSinglePrompt(prompt).collect { chunk ->
                    buffer.append(chunk)
                    _uiState.value = _uiState.value.copy(aiResponse = buffer.toString())
                }
                _uiState.value = _uiState.value.copy(isGeneratingAi = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingAi = false,
                    errorMessage = AiService.formatUserError(e)
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            pdfProcessor: PdfProcessor,
            aiService: AiService
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { PdfReaderViewModel(pdfProcessor, aiService) }
        }
    }
}
