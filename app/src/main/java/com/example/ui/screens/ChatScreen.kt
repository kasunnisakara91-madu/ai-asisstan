package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.db.MessageEntity
import com.example.data.local.preferences.AppLanguage
import com.example.ui.components.GlassCard
import com.example.ui.components.MarkdownMessageView
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalAiThemeTokens
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit,
    onNewChat: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onClearAllHistory: () -> Unit,
    onUpdateSearchQuery: (String) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onRegenerate: () -> Unit,
    onRetryFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAiThemeTokens.current
    val clipboard = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val itemCount = uiState.messages.size + if (uiState.streamingText.isNotBlank() || uiState.isGenerating) 1 else 0
    LaunchedEffect(itemCount, uiState.streamingText.length) {
        if (itemCount > 0) {
            listState.animateScrollToItem((itemCount - 1).coerceAtLeast(0))
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 900.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "Chat history",
                            tint = tokens.glowSecondary
                        )
                    }
                    Column {
                        Text(
                            text = "Gemini Neural Chat",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${uiState.activeModelName} • ${uiState.activeLanguage.code}",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.glowSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (uiState.messages.isNotEmpty() && !uiState.isGenerating) {
                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_regenerate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Regenerate last response",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onNewChat,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_new_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddComment,
                            contentDescription = "New conversation",
                            tint = tokens.glowSecondary
                        )
                    }
                }
            }

            // Language selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = uiState.activeLanguage == lang,
                        onClick = { onSelectLanguage(lang) },
                        label = {
                            Text(
                                text = lang.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tokens.glowPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = tokens.glowSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = uiState.activeLanguage == lang,
                            selectedBorderColor = tokens.glowSecondary
                        )
                    )
                }
            }

            // Conversation list or empty state
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (uiState.messages.isEmpty() && !uiState.isGenerating) {
                    ChatEmptyState(
                        onPickSuggestion = { suggestion ->
                            onSendMessage(suggestion)
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { msg ->
                            ChatMessageBubble(
                                message = msg,
                                onCopy = { clipboard.setText(AnnotatedString(msg.content)) },
                                onRetry = if (msg.isError) onRetryFailed else null
                            )
                        }
                        if (uiState.isGenerating) {
                            item(key = "streaming_bubble") {
                                StreamingAssistantBubble(text = uiState.streamingText)
                            }
                        }
                    }
                }
            }

            // Input bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = tokens.glassSurface,
                border = BorderStroke(1.dp, tokens.glassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input"),
                        placeholder = {
                            Text(
                                text = "Ask in English or සිංහල...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !uiState.isGenerating) {
                                    Brush.linearGradient(listOf(tokens.glowPrimary, tokens.glowSecondary))
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                            )
                            .clickable(enabled = inputText.isNotBlank() && !uiState.isGenerating) {
                                val toSend = inputText.trim()
                                inputText = ""
                                onSendMessage(toSend)
                            }
                            .testTag("chat_send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = tokens.glowSecondary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send message",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Chat History",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (uiState.conversations.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.testTag("history_clear_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                tint = CrimsonError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear All", color = CrimsonError)
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onUpdateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_history_input"),
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = "Search history")
                    },
                    placeholder = { Text("Search conversations or messages...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                if (uiState.conversations.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (uiState.searchQuery.isBlank()) {
                                "No saved conversations yet."
                            } else {
                                "No matching chats found for \"${uiState.searchQuery}\"."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.conversations, key = { it.id }) { conv ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                highlightBorder = conv.id == uiState.activeConversationId,
                                onClick = {
                                    onSelectConversation(conv.id)
                                    showHistorySheet = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = conv.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${conv.messageCount} msgs • ${formatRelativeTimestamp(conv.updatedAt)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = tokens.glowSecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteConversation(conv.id) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete chat",
                                            tint = CrimsonError
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all chat history?") },
            text = { Text("This permanently deletes all saved conversations from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllHistory()
                        showClearConfirm = false
                        showHistorySheet = false
                    }
                ) {
                    Text("Delete All", color = CrimsonError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ChatEmptyState(onPickSuggestion: (String) -> Unit) {
    val tokens = LocalAiThemeTokens.current
    val suggestions = listOf(
        "Explain quantum computing simply",
        "සිංහලෙන් කෘතිම බුද්ධිය (AI) ගැන කෙටි හැඳින්වීමක් දෙන්න",
        "Write a clean Kotlin Coroutines flow example",
        "Summarize tips for building fast Android Compose UIs"
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Image(
                painter = painterResource(id = R.drawable.img_ai_core_logo),
                contentDescription = "AI Core",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, tokens.glowSecondary, CircleShape)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Ask AI ASISSTANT",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Communicate naturally in Sinhala or English with real-time Gemini streaming.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(suggestions) { suggestion ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onPickSuggestion(suggestion) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = tokens.glowSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: MessageEntity,
    onCopy: () -> Unit,
    onRetry: (() -> Unit)?
) {
    val tokens = LocalAiThemeTokens.current
    val isUser = message.role == "user"
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            Surface(
                modifier = Modifier.widthIn(max = 560.dp),
                shape = RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 6.dp,
                    bottomStart = 22.dp,
                    bottomEnd = 22.dp
                ),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(tokens.glowPrimary, tokens.glowSecondary.copy(alpha = 0.88f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }
        } else {
            GlassCard(
                modifier = Modifier.fillMaxWidth(0.96f),
                shape = RoundedCornerShape(
                    topStart = 6.dp,
                    topEnd = 22.dp,
                    bottomStart = 22.dp,
                    bottomEnd = 22.dp
                ),
                highlightBorder = message.isError
            ) {
                if (message.isError) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = CrimsonError
                    )
                    if (onRetry != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = onRetry,
                            modifier = Modifier.testTag("chat_retry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Retry request",
                                tint = tokens.glowSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Request", color = tokens.glowSecondary)
                        }
                    }
                } else {
                    MarkdownMessageView(text = message.content)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatRelativeTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = {
                                onCopy()
                                copied = true
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                contentDescription = "Copy response",
                                tint = if (copied) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingAssistantBubble(text: String) {
    val tokens = LocalAiThemeTokens.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(0.96f),
        highlightBorder = true
    ) {
        if (text.isBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = tokens.glowSecondary
                )
                Text(
                    text = "Thinking with Gemini...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.glowSecondary
                )
            }
        } else {
            MarkdownMessageView(text = text)
        }
    }
}
