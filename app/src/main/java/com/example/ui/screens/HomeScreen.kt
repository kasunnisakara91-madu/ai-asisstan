package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.db.ConversationSummary
import com.example.data.local.preferences.AppSettings
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGlowButton
import com.example.ui.components.NeonStatusPill
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalAiThemeTokens

private data class HomeQuickTool(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    settings: AppSettings,
    recentConversations: List<ConversationSummary>,
    onStartNewChat: () -> Unit,
    onOpenConversation: (Long) -> Unit,
    onOpenVoice: () -> Unit,
    onOpenImageGen: () -> Unit,
    onOpenCoding: () -> Unit,
    onOpenPdfReader: () -> Unit,
    onOpenWebSearch: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAiThemeTokens.current
    val tools = listOf(
        HomeQuickTool(
            title = "Image Studio",
            subtitle = "Create & save AI art",
            icon = Icons.Rounded.Image,
            accent = tokens.glowSecondary,
            tag = "home_tool_image",
            onClick = onOpenImageGen
        ),
        HomeQuickTool(
            title = "Code Studio",
            subtitle = "9 languages + debug",
            icon = Icons.Rounded.Code,
            accent = CyberCyan,
            tag = "home_tool_code",
            onClick = onOpenCoding
        ),
        HomeQuickTool(
            title = "PDF Reader",
            subtitle = "Extract & translate",
            icon = Icons.Rounded.PictureAsPdf,
            accent = EmeraldSuccess,
            tag = "home_tool_pdf",
            onClick = onOpenPdfReader
        ),
        HomeQuickTool(
            title = "Web Search",
            subtitle = "Live sources + summary",
            icon = Icons.Rounded.TravelExplore,
            accent = Color(0xFFFBBF24),
            tag = "home_tool_search",
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_ai_core_logo),
                                contentDescription = "AI ASISSTANT emblem",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, tokens.glowSecondary, CircleShape)
                            )
                            Column {
                                Text(
                                    text = "AI ASISSTANT",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Welcome back, ${settings.profileName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenProfile,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_profile_button")
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_ai_avatar),
                                contentDescription = "Open Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, tokens.glowPrimary, CircleShape)
                            )
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .border(
                                1.2.dp,
                                Brush.linearGradient(listOf(tokens.glowSecondary, tokens.glowPrimary)),
                                RoundedCornerShape(26.dp)
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner),
                            contentDescription = "Futuristic neural hero banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x8806050A),
                                            Color(0xE80A0812)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NeonStatusPill(
                                    label = settings.geminiModelName.uppercase(),
                                    accentColor = tokens.glowSecondary
                                )
                                NeonStatusPill(
                                    label = "සිංහල + EN",
                                    accentColor = CyberCyan
                                )
                            }

                            Text(
                                text = "Think Faster. Create Smarter.",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Bilingual Gemini intelligence, live voice dialogue, code engineering, PDF analysis, and web research.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD8CFF0),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NeonGlowButton(
                                    text = "New AI Chat",
                                    icon = Icons.Rounded.AutoAwesome,
                                    onClick = onStartNewChat,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_new_chat_button")
                                )
                                Surface(
                                    modifier = Modifier
                                        .height(52.dp)
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(onClick = onOpenVoice)
                                        .testTag("home_voice_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xCC1B142B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        tokens.glowSecondary.copy(alpha = 0.55f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.GraphicEq,
                                            contentDescription = "Voice Assistant",
                                            tint = tokens.glowSecondary
                                        )
                                        Text(
                                            text = "Voice",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "AI Power Suite",
                        subtitle = "Launch native creative & engineering modules"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            tools.forEach { tool ->
                                QuickToolCard(tool = tool, modifier = Modifier.weight(1f))
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            tools.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    pair.forEach { tool ->
                                        QuickToolCard(tool = tool, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "Recent Conversations",
                        subtitle = if (recentConversations.isEmpty()) {
                            "Your encrypted local Room chat log starts here"
                        } else {
                            "${recentConversations.size} saved chats on device"
                        }
                    )
                }

                if (recentConversations.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Chat,
                                    contentDescription = null,
                                    tint = tokens.glowSecondary,
                                    modifier = Modifier.size(30.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No conversations yet",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Tap New AI Chat to ask questions in Sinhala or English.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(recentConversations.take(6), key = { it.id }) { item ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recent_chat_${item.id}"),
                            onClick = { onOpenConversation(item.id) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!item.lastMessagePreview.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = item.lastMessagePreview,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatRelativeTimestamp(item.updatedAt),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = tokens.glowSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = "Open chat",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToolCard(tool: HomeQuickTool, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier.testTag(tool.tag),
        onClick = tool.onClick
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tool.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.title,
                tint = tool.accent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = tool.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tool.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
