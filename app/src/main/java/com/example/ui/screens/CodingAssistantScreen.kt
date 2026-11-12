package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.MarkdownMessageView
import com.example.ui.components.NeonGlowButton
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAiThemeTokens

@Composable
fun CodingAssistantScreen(
    uiState: CodingUiState,
    onSelectLanguage: (CodingLanguage) -> Unit,
    onSelectTask: (CodingTask) -> Unit,
    onPromptChange: (String) -> Unit,
    onSourceCodeChange: (String) -> Unit,
    onRunTask: () -> Unit,
    onFormatAction: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAiThemeTokens.current
    val clipboard = LocalClipboardManager.current

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text("AI Coding Studio", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Generate • Explain • Debug • Fix • Format",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.glowSecondary
                        )
                    }
                }
            }

            item {
                Text("Language", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CodingLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = uiState.selectedLanguage == lang,
                            onClick = { onSelectLanguage(lang) },
                            label = { Text(lang.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = tokens.glowPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = tokens.glowSecondary
                            )
                        )
                    }
                }
            }

            item {
                Text("Workflow Mode", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CodingTask.entries.forEach { task ->
                        FilterChip(
                            selected = uiState.selectedTask == task,
                            onClick = { onSelectTask(task) },
                            label = { Text(task.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f),
                                selectedLabelColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.promptInput,
                        onValueChange = onPromptChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("coding_prompt_input"),
                        label = { Text("Task prompt or error message") },
                        placeholder = {
                            Text("e.g., Build a Retrofit repository with exponential backoff retry")
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = uiState.sourceCodeInput,
                        onValueChange = onSourceCodeChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("coding_source_input"),
                        label = { Text("Optional source code (${uiState.selectedLanguage.displayName})") },
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp
                        ),
                        minLines = 4,
                        maxLines = 9,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeonGlowButton(
                            text = if (uiState.isGenerating) "Running..." else "${uiState.selectedTask.label} ${uiState.selectedLanguage.displayName}",
                            icon = if (uiState.selectedTask == CodingTask.FORMAT) Icons.Rounded.AutoFixHigh else Icons.Rounded.Code,
                            enabled = !uiState.isGenerating,
                            onClick = {
                                if (uiState.selectedTask == CodingTask.FORMAT) onFormatAction()
                                else onRunTask()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("coding_run_button")
                        )
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrimsonError
                        )
                    }
                }
            }

            if (uiState.isGenerating || uiState.outputMarkdown.isNotBlank()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (uiState.isGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = tokens.glowSecondary
                                    )
                                }
                                Text(
                                    text = "Assistant Output",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.glowSecondary
                                )
                            }
                            if (uiState.outputMarkdown.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(uiState.outputMarkdown))
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy full output",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy All")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.outputMarkdown.isBlank()) {
                            Text(
                                text = "Synthesizing code with Gemini...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            MarkdownMessageView(text = uiState.outputMarkdown)
                        }
                    }
                }
            }
        }
    }
}
