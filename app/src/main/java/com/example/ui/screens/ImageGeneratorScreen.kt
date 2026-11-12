package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGlowButton
import com.example.ui.components.NeonStatusPill
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalAiThemeTokens
import kotlinx.coroutines.launch

@Composable
fun ImageGeneratorScreen(
    uiState: ImageGeneratorUiState,
    onPromptChange: (String) -> Unit,
    onSelectStyle: (String) -> Unit,
    onGenerate: () -> Unit,
    onSaveImage: (android.graphics.Bitmap, android.net.Uri?) -> Unit,
    onPrepareShare: suspend (android.graphics.Bitmap) -> android.content.Intent?,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalAiThemeTokens.current
    val scope = rememberCoroutineScope()
    val styles = listOf("Futuristic Neon", "Photorealistic", "Cyberpunk Concept", "Minimalist 3D", "Natural")

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val current = uiState.result
        if (uri != null && current is ImageGenResultState.Success) {
            onSaveImage(current.bitmap, uri)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Column {
                            Text("AI Image Studio", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = uiState.provider.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.glowSecondary
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Configure image API",
                            tint = tokens.glowSecondary
                        )
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.prompt,
                        onValueChange = onPromptChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_prompt_input"),
                        label = { Text("Describe the image to generate") },
                        placeholder = { Text("e.g., Neon purple lotus floating over futuristic Colombo skyline at night") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        styles.forEach { style ->
                            FilterChip(
                                selected = uiState.selectedStyle == style,
                                onClick = { onSelectStyle(style) },
                                label = { Text(style) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tokens.glowPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = tokens.glowSecondary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    NeonGlowButton(
                        text = if (uiState.result is ImageGenResultState.Loading) "Synthesizing..." else "Generate Image",
                        icon = Icons.Rounded.AutoAwesome,
                        enabled = uiState.prompt.isNotBlank() && uiState.result !is ImageGenResultState.Loading,
                        onClick = onGenerate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_generate_button")
                    )
                }
            }

            item {
                when (val state = uiState.result) {
                    is ImageGenResultState.Idle -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NeonStatusPill("REAL IMAGE API READY")
                                Text(
                                    text = "Enter a detailed visual prompt above.",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Uses Firebase AI Nano Banana 2.1 or your configured OpenAI-compatible image provider in Settings.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is ImageGenResultState.Loading -> {
                        val shimmerTrans = rememberInfiniteTransition(label = "imgShimmer")
                        val alpha by shimmerTrans.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 0.55f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "shimmerAlpha"
                        )
                        GlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                tokens.glowPrimary.copy(alpha = alpha),
                                                tokens.glowSecondary.copy(alpha = alpha * 0.6f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = tokens.glowSecondary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Generating artwork (may take 30–60s)...",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }

                    is ImageGenResultState.Success -> {
                        GlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                            Image(
                                bitmap = state.bitmap.asImageBitmap(),
                                contentDescription = "Generated AI image for ${state.prompt}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            onSaveImage(state.bitmap, null)
                                        } else {
                                            createDocLauncher.launch("AI_ASISSTANT_${System.currentTimeMillis()}.png")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("image_save_button")
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save")
                                }
                                NeonGlowButton(
                                    text = "Share",
                                    icon = Icons.Rounded.Share,
                                    onClick = {
                                        scope.launch {
                                            val shareIntent = onPrepareShare(state.bitmap)
                                            if (shareIntent != null) {
                                                context.startActivity(
                                                    android.content.Intent.createChooser(
                                                        shareIntent,
                                                        "Share AI Image"
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("image_share_button"),
                                    height = 48.dp
                                )
                            }
                            if (!uiState.actionFeedback.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.actionFeedback,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }

                    is ImageGenResultState.Error -> {
                        GlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = "Provider error",
                                    tint = CrimsonError
                                )
                                Text(
                                    text = "Image Generation Unavailable",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CrimsonError
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.testTag("image_open_settings_button")
                            ) {
                                Icon(Icons.Rounded.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Configure Image Provider in Settings")
                            }
                        }
                    }
                }
            }
        }
    }
}
