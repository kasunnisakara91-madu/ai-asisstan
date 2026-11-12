package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.components.MarkdownMessageView
import com.example.ui.components.NeonGlowButton
import com.example.ui.components.NeonStatusPill
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalAiThemeTokens

@Composable
fun PdfReaderScreen(
    uiState: PdfReaderUiState,
    onPickPdfUri: (android.net.Uri) -> Unit,
    onGoToPage: (Int) -> Unit,
    onToggleViewMode: () -> Unit,
    onGrantConsent: () -> Unit,
    onQuestionChange: (String) -> Unit,
    onRunAiAction: (PdfAiAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAiThemeTokens.current
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onPickPdfUri(uri)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 900.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text("AI PDF Reader", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "On-device extraction • Bilingual Gemini analysis",
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.glowSecondary
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pdf_pick_button")
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (uiState.document == null) "Open PDF" else "Change")
                    }
                }
            }

            if (uiState.isLoadingPdf) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = tokens.glowSecondary)
                            Text("Extracting text locally on device...", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (uiState.document == null && !uiState.isLoadingPdf) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(46.dp)
                            )
                            Text("Select a PDF Document", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = stringResource(id = R.string.pdf_privacy_notice),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            NeonGlowButton(
                                text = "Choose PDF File",
                                icon = Icons.Rounded.UploadFile,
                                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.testTag("pdf_empty_open_button")
                            )
                        }
                    }
                }
            }

            val doc = uiState.document
            if (doc != null) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.fileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Page ${uiState.currentPageIndex + 1} of ${doc.pageCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tokens.glowSecondary
                                )
                            }
                            TextButton(onClick = onToggleViewMode) {
                                Text(if (uiState.showPagePreview) "View Text" else "View Page")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.showPagePreview && uiState.currentPageBitmap != null) {
                            Image(
                                bitmap = uiState.currentPageBitmap.asImageBitmap(),
                                contentDescription = "PDF Page ${uiState.currentPageIndex + 1}",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            val pageText = doc.pageTexts.getOrNull(uiState.currentPageIndex)
                                ?.ifBlank { "(No selectable text on this page)" }
                                ?: ""
                            Text(
                                text = pageText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { onGoToPage(uiState.currentPageIndex - 1) },
                                enabled = uiState.currentPageIndex > 0,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.NavigateBefore, contentDescription = "Previous page")
                                Text("Prev")
                            }
                            NeonStatusPill("${uiState.currentPageIndex + 1} / ${doc.pageCount}")
                            OutlinedButton(
                                onClick = { onGoToPage(uiState.currentPageIndex + 1) },
                                enabled = uiState.currentPageIndex < doc.pageCount - 1,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Next")
                                Icon(Icons.AutoMirrored.Rounded.NavigateNext, contentDescription = "Next page")
                            }
                        }
                    }
                }

                // Privacy & Consent Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        highlightBorder = !uiState.consentGrantedForDoc
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.consentGrantedForDoc) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = if (uiState.consentGrantedForDoc) EmeraldSuccess else tokens.glowSecondary
                            )
                            Text(
                                text = if (uiState.consentGrantedForDoc) {
                                    "AI Document Processing Enabled"
                                } else {
                                    "Document Privacy Gate"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(id = R.string.pdf_privacy_notice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!uiState.consentGrantedForDoc) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onGrantConsent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("pdf_consent_button")
                            ) {
                                Text("Allow AI Analysis for This Document")
                            }
                        }
                    }
                }

                if (uiState.consentGrantedForDoc) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("AI Document Tools", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onRunAiAction(PdfAiAction.SUMMARIZE) },
                                    enabled = !uiState.isGeneratingAi,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("pdf_summarize_button")
                                ) {
                                    Icon(Icons.Rounded.Summarize, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Summarize", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { onRunAiAction(PdfAiAction.TRANSLATE_SINHALA) },
                                    enabled = !uiState.isGeneratingAi,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("pdf_translate_si_button")
                                ) {
                                    Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("සිංහල", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { onRunAiAction(PdfAiAction.TRANSLATE_ENGLISH) },
                                    enabled = !uiState.isGeneratingAi,
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("English", maxLines = 1)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = uiState.questionInput,
                                    onValueChange = onQuestionChange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pdf_question_input"),
                                    placeholder = { Text("Ask a question about this PDF...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                IconButton(
                                    onClick = { onRunAiAction(PdfAiAction.ASK_QUESTION) },
                                    enabled = uiState.questionInput.isNotBlank() && !uiState.isGeneratingAi,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("pdf_ask_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "Ask about PDF",
                                        tint = tokens.glowSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                        Text(uiState.errorMessage, color = CrimsonError)
                    }
                }
            }

            if (uiState.isGeneratingAi || uiState.aiResponse.isNotBlank()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.isGeneratingAi) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = tokens.glowSecondary
                                )
                            }
                            Text(
                                text = "Gemini Document Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.glowSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        MarkdownMessageView(text = uiState.aiResponse)
                    }
                }
            }
        }
    }
}
