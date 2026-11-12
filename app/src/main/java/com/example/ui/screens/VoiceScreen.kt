package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.components.GlassCard
import com.example.ui.components.MarkdownMessageView
import com.example.ui.components.NeonStatusPill
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.LocalAiThemeTokens
import kotlin.math.sin

@Composable
fun VoiceScreen(
    uiState: VoiceUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSelectLocale: (String) -> Unit,
    onToggleTts: () -> Unit,
    onSpeakText: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onSubmitQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalAiThemeTokens.current
    var fallbackInput by remember { mutableStateOf("") }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) onStartListening()
    }

    val animatedRms by animateFloatAsState(
        targetValue = if (uiState.engine.isListening) uiState.engine.rmsLevel.coerceAtLeast(0.18f)
        else if (uiState.engine.isSpeaking) 0.45f
        else 0.08f,
        animationSpec = tween(140),
        label = "rmsAnim"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "voiceOrb")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Voice Assistant",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Real SpeechRecognizer + Gemini + Text-to-Speech",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onToggleTts,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("voice_tts_toggle")
                    ) {
                        Icon(
                            imageVector = if (uiState.ttsEnabled) {
                                Icons.AutoMirrored.Rounded.VolumeUp
                            } else {
                                Icons.AutoMirrored.Rounded.VolumeOff
                            },
                            contentDescription = if (uiState.ttsEnabled) "Voice playback on" else "Voice playback muted",
                            tint = if (uiState.ttsEnabled) tokens.glowSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val locales = listOf(
                        "auto" to "Auto Detect",
                        "en-US" to "English (US)",
                        "si-LK" to "සිංහල (si-LK)"
                    )
                    locales.forEach { (code, label) ->
                        FilterChip(
                            selected = uiState.selectedLocale == code,
                            onClick = { onSelectLocale(code) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = tokens.glowPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = tokens.glowSecondary
                            )
                        )
                    }
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    highlightBorder = uiState.engine.isListening || uiState.engine.isSpeaking
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NeonStatusPill(
                            label = when {
                                uiState.engine.isListening -> "LISTENING LIVE"
                                uiState.isGenerating -> "THINKING WITH GEMINI"
                                uiState.engine.isSpeaking -> "SPEAKING RESPONSE"
                                else -> "READY FOR VOICE"
                            },
                            accentColor = if (uiState.engine.isListening) CyberCyan else tokens.glowSecondary
                        )

                        // Animated Waveform & Orbital Visualizer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val baseRadius = size.minDimension * 0.28f
                                val pulseRadius = baseRadius + (animatedRms * 34f)

                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            tokens.glowPrimary.copy(alpha = 0.28f + animatedRms * 0.3f),
                                            Color.Transparent
                                        ),
                                        center = center,
                                        radius = pulseRadius * 1.9f
                                    ),
                                    center = center,
                                    radius = pulseRadius * 1.9f
                                )
                                drawCircle(
                                    color = tokens.glowSecondary.copy(alpha = 0.55f),
                                    radius = pulseRadius,
                                    center = center,
                                    style = Stroke(width = 3.dp.toPx())
                                )

                                // Equalizer bars flanking the orb
                                val barCount = 18
                                val barSpacing = size.width / (barCount + 4)
                                for (i in 0 until barCount) {
                                    val x = (i + 2.5f) * barSpacing
                                    val wave = sin(wavePhase + i * 0.55f)
                                    val barHeight = (10f + animatedRms * 64f * (0.5f + 0.5f * wave))
                                        .coerceIn(8f, size.height * 0.7f)
                                    drawLine(
                                        color = tokens.glowSecondary.copy(alpha = 0.45f),
                                        start = Offset(x, center.y - barHeight / 2f),
                                        end = Offset(x, center.y + barHeight / 2f),
                                        strokeWidth = 4.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .shadow(
                                        elevation = 18.dp,
                                        shape = CircleShape,
                                        ambientColor = tokens.glowPrimary,
                                        spotColor = tokens.glowSecondary
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            if (uiState.engine.isListening) {
                                                listOf(CrimsonError, tokens.glowPrimary)
                                            } else {
                                                listOf(tokens.glowPrimary, tokens.glowSecondary)
                                            }
                                        )
                                    )
                                    .minimumInteractiveComponentSize()
                                    .clickable {
                                        if (uiState.engine.isListening) {
                                            onStopListening()
                                        } else if (hasAudioPermission) {
                                            onStartListening()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                    .testTag("voice_mic_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.engine.isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                    contentDescription = if (uiState.engine.isListening) "Stop listening" else "Start listening",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        if (uiState.engine.partialTranscript.isNotBlank()) {
                            Text(
                                text = "\"${uiState.engine.partialTranscript}\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.glowSecondary
                            )
                        }

                        if (uiState.engine.isSpeaking) {
                            TextButton(onClick = onStopSpeaking) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop speaking", tint = CrimsonError)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop Playback", color = CrimsonError)
                            }
                        }

                        if (!hasAudioPermission) {
                            Text(
                                text = "Tap the microphone to grant RECORD_AUDIO permission for live speech recognition.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!uiState.engine.isRecognitionAvailable) {
                            Text(
                                text = "Device SpeechRecognizer service is not installed here. Use the prompt box below to test Gemini + Text-to-Speech playback.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFBBF24)
                            )
                        }

                        if (!uiState.errorMessage.isNullOrBlank()) {
                            Text(
                                text = uiState.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CrimsonError
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fallbackInput,
                        onValueChange = { fallbackInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_text_input"),
                        placeholder = { Text("Or type a spoken query (EN / සිංහල)...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    IconButton(
                        onClick = {
                            val q = fallbackInput.trim()
                            if (q.isNotEmpty()) {
                                fallbackInput = ""
                                onSubmitQuery(q)
                            }
                        },
                        enabled = fallbackInput.isNotBlank() && !uiState.isGenerating,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(tokens.glowPrimary.copy(alpha = 0.25f))
                            .testTag("voice_send_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Ask voice assistant",
                            tint = tokens.glowSecondary
                        )
                    }
                }
            }

            items(uiState.turns, key = { it.id }) { turn ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "YOU",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan
                    )
                    Text(
                        text = turn.userText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "AI ASISSTANT",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.glowSecondary
                        )
                        if (!turn.isStreaming && turn.assistantText.isNotBlank()) {
                            IconButton(
                                onClick = { onSpeakText(turn.assistantText) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = "Speak answer aloud",
                                    tint = tokens.glowSecondary
                                )
                            }
                        }
                    }
                    if (turn.assistantText.isBlank()) {
                        Text(
                            text = "Generating spoken response...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        MarkdownMessageView(text = turn.assistantText)
                    }
                }
            }
        }
    }
}
