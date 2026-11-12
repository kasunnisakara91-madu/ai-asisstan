package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonStatusPill
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalAiThemeTokens

private data class ToolModuleCard(
    val title: String,
    val badge: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
fun ToolsScreen(
    onOpenImageGenerator: () -> Unit,
    onOpenCodingAssistant: () -> Unit,
    onOpenPdfReader: () -> Unit,
    onOpenWebSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAiThemeTokens.current
    val modules = listOf(
        ToolModuleCard(
            title = "AI Image Generator",
            badge = "IMAGE STUDIO",
            description = "Synthesize art via Nano Banana 2.1 or OpenAI-compatible APIs, preview, save, and share.",
            icon = Icons.Rounded.Image,
            accent = tokens.glowSecondary,
            tag = "tools_image_generator_card",
            onClick = onOpenImageGenerator
        ),
        ToolModuleCard(
            title = "AI Coding Assistant",
            badge = "9 LANGUAGES",
            description = "Generate, explain, debug, fix, and format Kotlin, Python, JS, HTML, CSS, Java, C++, PHP, and JSON.",
            icon = Icons.Rounded.Code,
            accent = CyberCyan,
            tag = "tools_coding_assistant_card",
            onClick = onOpenCodingAssistant
        ),
        ToolModuleCard(
            title = "AI PDF Reader",
            badge = "LOCAL + GEMINI",
            description = "Open PDFs securely on-device, navigate rendered pages, extract text, summarize, translate, and Q&A.",
            icon = Icons.Rounded.PictureAsPdf,
            accent = EmeraldSuccess,
            tag = "tools_pdf_reader_card",
            onClick = onOpenPdfReader
        ),
        ToolModuleCard(
            title = "AI Web Search",
            badge = "LIVE SOURCES",
            description = "Query real search APIs, inspect source titles and URLs, open links, and read grounded Gemini summaries.",
            icon = Icons.Rounded.TravelExplore,
            accent = Color(0xFFFBBF24),
            tag = "tools_web_search_card",
            onClick = onOpenWebSearch
        )
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 920.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "AI Tools Suite",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a specialized workspace powered by Gemini & native Android APIs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isWide) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            modules.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    pair.forEach { card ->
                                        ToolCardItem(card = card, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(modules.size) { index ->
                        ToolCardItem(card = modules[index], modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCardItem(card: ToolModuleCard, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier.testTag(card.tag),
        highlightBorder = true,
        onClick = card.onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(card.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = card.icon,
                    contentDescription = card.title,
                    tint = card.accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            NeonStatusPill(label = card.badge, accentColor = card.accent)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = card.title,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = card.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Launch Module",
                style = MaterialTheme.typography.labelLarge,
                color = card.accent
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = card.accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
