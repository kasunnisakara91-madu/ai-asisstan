package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard
import com.example.ui.components.MarkdownMessageView
import com.example.ui.components.NeonGlowButton
import com.example.ui.components.NeonStatusPill
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.LocalAiThemeTokens

@Composable
fun WebSearchScreen(
    uiState: WebSearchUiState,
    onQueryChange: (String) -> Unit,
    onExecuteSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalAiThemeTokens.current

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
                            Text("AI Web Search", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = uiState.activeProvider.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "Search API configuration",
                            tint = tokens.glowSecondary
                        )
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("web_search_input"),
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        label = { Text("Search query") },
                        placeholder = { Text("Search live web knowledge...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NeonGlowButton(
                        text = if (uiState.isSearching) "Searching..." else "Search & Summarize",
                        icon = Icons.Rounded.Search,
                        enabled = uiState.query.isNotBlank() && !uiState.isSearching,
                        onClick = onExecuteSearch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("web_search_button")
                    )
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                        Text(
                            text = uiState.errorMessage,
                            color = CrimsonError,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (uiState.needsConfiguration) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onOpenSettings) {
                                Icon(Icons.Rounded.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Configure Search API in Settings")
                            }
                        }
                    }
                }
            }

            if (uiState.isSummarizing || uiState.aiSummary.isNotBlank()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.isSummarizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = tokens.glowSecondary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = tokens.glowSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Gemini Grounded Summary",
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.glowSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.aiSummary.isBlank()) {
                            Text("Synthesizing retrieved sources...")
                        } else {
                            MarkdownMessageView(text = uiState.aiSummary)
                        }
                    }
                }
            }

            if (uiState.hasSearched && uiState.results.isEmpty() && uiState.errorMessage == null) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No results returned for \"${uiState.query}\". Try broader keywords.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (uiState.results.isNotEmpty()) {
                item {
                    Text(
                        text = "Retrieved Sources (${uiState.results.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(uiState.results) { result ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (result.url.startsWith("https://") || result.url.startsWith("http://")) {
                                runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.url))
                                    context.startActivity(intent)
                                }
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NeonStatusPill(
                                label = result.sourceLabel,
                                accentColor = CyberCyan
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = "Open source website",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = result.url,
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.glowSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (result.snippet.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = result.snippet,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
