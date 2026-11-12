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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.preferences.AppLanguage
import com.example.data.local.preferences.ImageGenProvider
import com.example.data.local.preferences.WebSearchProvider
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonStatusPill
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalAiThemeTokens
import com.example.ui.theme.ThemePreset
import java.util.Locale

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateTheme: (ThemePreset, Float) -> Unit,
    onUpdateVoice: (Boolean, Float, Float, String) -> Unit,
    onSaveGeminiConfig: (String, Boolean, String?) -> Unit,
    onClearGeminiKey: () -> Unit,
    onSaveImageConfig: (ImageGenProvider, String, String, String?) -> Unit,
    onClearImageKey: () -> Unit,
    onSaveSearchConfig: (WebSearchProvider, String, String?) -> Unit,
    onClearSearchKey: () -> Unit,
    onClearAllHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    onDismissToast: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAiThemeTokens.current
    val settings = uiState.settings
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.toastMessage) {
        val msg = uiState.toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            onDismissToast()
        }
    }

    var geminiModel by remember(settings.geminiModelName) { mutableStateOf(settings.geminiModelName) }
    var useCustomGemini by remember(settings.useCustomGeminiKey) { mutableStateOf(settings.useCustomGeminiKey) }
    var geminiKeyDraft by remember { mutableStateOf("") }
    var showGeminiKey by remember { mutableStateOf(false) }

    var imgProvider by remember(settings.imageGenProvider) { mutableStateOf(settings.imageGenProvider) }
    var imgEndpoint by remember(settings.imageGenEndpoint) { mutableStateOf(settings.imageGenEndpoint) }
    var imgModel by remember(settings.imageGenModel) { mutableStateOf(settings.imageGenModel) }
    var imgKeyDraft by remember { mutableStateOf("") }

    var searchProvider by remember(settings.webSearchProvider) { mutableStateOf(settings.webSearchProvider) }
    var searchEngineId by remember(settings.webSearchEngineId) { mutableStateOf(settings.webSearchEngineId) }
    var searchKeyDraft by remember { mutableStateOf("") }

    var ttsRate by remember(settings.ttsSpeechRate) { mutableFloatStateOf(settings.ttsSpeechRate) }
    var ttsPitch by remember(settings.ttsPitch) { mutableFloatStateOf(settings.ttsPitch) }
    var glowIntensity by remember(settings.glowIntensity) { mutableFloatStateOf(settings.glowIntensity) }

    var showClearConfirm by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 900.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Settings & Security", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Android Keystore AES-256-GCM encrypted credentials",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.glowSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenProfile,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("settings_profile_button")
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Profile")
                    }
                }
            }

            // 1. Gemini API Key & Model Config
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                    SettingsSectionTitle(Icons.Rounded.Key, "Gemini API Configuration")
                    Spacer(modifier = Modifier.height(6.dp))
                    NeonStatusPill(
                        label = if (useCustomGemini && settings.hasGeminiApiKey) {
                            "CUSTOM ENCRYPTED KEY (${settings.geminiApiKeyMasked})"
                        } else {
                            "FIREBASE AI LOGIC (APP CHECK ACTIVE)"
                        },
                        accentColor = EmeraldSuccess
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Model", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-4-argon").forEach { m ->
                            FilterChip(
                                selected = geminiModel == m,
                                onClick = {
                                    geminiModel = m
                                    onSaveGeminiConfig(m, useCustomGemini, null)
                                },
                                label = { Text(m) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use Custom Gemini API Key", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Optional Keystore-encrypted override for standalone builds",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useCustomGemini,
                            onCheckedChange = {
                                useCustomGemini = it
                                onSaveGeminiConfig(geminiModel, it, null)
                            },
                            modifier = Modifier.testTag("settings_custom_gemini_switch")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = geminiKeyDraft,
                        onValueChange = { geminiKeyDraft = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_gemini_key_input"),
                        label = { Text("Enter new Gemini API Key (${settings.geminiApiKeyMasked})") },
                        visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Icon(
                                    imageVector = if (showGeminiKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Toggle key visibility"
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSaveGeminiConfig(geminiModel, useCustomGemini, geminiKeyDraft)
                                geminiKeyDraft = ""
                            },
                            enabled = geminiKeyDraft.isNotBlank(),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Encrypt & Save Key")
                        }
                        if (settings.hasGeminiApiKey) {
                            OutlinedButton(
                                onClick = onClearGeminiKey,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Clear Key", color = CrimsonError)
                            }
                        }
                    }
                }
            }

            // 2. Image Generation Config
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsSectionTitle(Icons.Rounded.Palette, "Image Generation API Configuration")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ImageGenProvider.entries.forEach { p ->
                            FilterChip(
                                selected = imgProvider == p,
                                onClick = {
                                    imgProvider = p
                                    onSaveImageConfig(p, imgEndpoint, imgModel, null)
                                },
                                label = { Text(p.label) }
                            )
                        }
                    }
                    if (imgProvider == ImageGenProvider.OPENAI_COMPATIBLE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = imgEndpoint,
                            onValueChange = { imgEndpoint = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Image API Endpoint URL") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = imgModel,
                            onValueChange = { imgModel = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Model ID (e.g., gpt-image-1 / dall-e-3)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = imgKeyDraft,
                            onValueChange = { imgKeyDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Image API Key (${settings.imageGenApiKeyMasked})") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onSaveImageConfig(
                                        imgProvider,
                                        imgEndpoint,
                                        imgModel,
                                        imgKeyDraft.takeIf { it.isNotBlank() }
                                    )
                                    imgKeyDraft = ""
                                },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Save Image Config")
                            }
                            if (settings.hasImageGenApiKey) {
                                OutlinedButton(onClick = onClearImageKey, modifier = Modifier.height(48.dp)) {
                                    Text("Clear", color = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Web Search API Config
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsSectionTitle(Icons.Rounded.Security, "Web Search API Configuration")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WebSearchProvider.entries.forEach { p ->
                            FilterChip(
                                selected = searchProvider == p,
                                onClick = {
                                    searchProvider = p
                                    onSaveSearchConfig(p, searchEngineId, null)
                                },
                                label = { Text(p.label) }
                            )
                        }
                    }
                    if (searchProvider.requiresApiKey) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (searchProvider == WebSearchProvider.GOOGLE_CUSTOM_SEARCH) {
                            OutlinedTextField(
                                value = searchEngineId,
                                onValueChange = { searchEngineId = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Google Custom Search Engine ID (cx)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedTextField(
                            value = searchKeyDraft,
                            onValueChange = { searchKeyDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search API Key (${settings.webSearchApiKeyMasked})") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onSaveSearchConfig(
                                        searchProvider,
                                        searchEngineId,
                                        searchKeyDraft.takeIf { it.isNotBlank() }
                                    )
                                    searchKeyDraft = ""
                                },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Save Search Config")
                            }
                            if (settings.hasWebSearchApiKey) {
                                OutlinedButton(onClick = onClearSearchKey, modifier = Modifier.height(48.dp)) {
                                    Text("Clear", color = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Language & Voice Settings
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsSectionTitle(Icons.Rounded.Language, "Language & Voice")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = settings.appLanguage == lang,
                                onClick = { onUpdateLanguage(lang) },
                                label = { Text(lang.label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speak Voice Responses (TTS)", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = settings.ttsEnabled,
                            onCheckedChange = {
                                onUpdateVoice(it, ttsRate, ttsPitch, settings.voiceLocale)
                            }
                        )
                    }
                    Text("Speech Rate: ${String.format(Locale.US, "%.2fx", ttsRate)}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = ttsRate,
                        onValueChange = { ttsRate = it },
                        onValueChangeFinished = {
                            onUpdateVoice(settings.ttsEnabled, ttsRate, ttsPitch, settings.voiceLocale)
                        },
                        valueRange = 0.6f..1.6f
                    )
                    Text("Voice Pitch: ${String.format(Locale.US, "%.2f", ttsPitch)}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = ttsPitch,
                        onValueChange = { ttsPitch = it },
                        onValueChangeFinished = {
                            onUpdateVoice(settings.ttsEnabled, ttsRate, ttsPitch, settings.voiceLocale)
                        },
                        valueRange = 0.7f..1.4f
                    )
                }
            }

            // 5. Theme Settings
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsSectionTitle(Icons.Rounded.Palette, "Theme & Glow")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = settings.themePreset == preset,
                                onClick = { onUpdateTheme(preset, glowIntensity) },
                                label = { Text(preset.displayName) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Neon Glow Intensity: ${(glowIntensity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = glowIntensity,
                        onValueChange = { glowIntensity = it },
                        onValueChangeFinished = { onUpdateTheme(settings.themePreset, glowIntensity) },
                        valueRange = 0.4f..1.5f
                    )
                }
            }

            // 6. Data, Privacy & About
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsSectionTitle(Icons.Rounded.Info, "Storage, Privacy & About")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Stored locally in Room: ${uiState.conversationCount} conversations (${uiState.messageCount} messages).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Privacy Info")
                        }
                        Button(
                            onClick = { showClearConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonError),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_clear_history_button")
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Chats")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "AI ASISSTANT • v${BuildConfig.VERSION_NAME} (Native Kotlin & Jetpack Compose)",
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.glowSecondary
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all chat history?") },
            text = { Text("All saved conversations and messages in the Room database will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllHistory()
                        showClearConfirm = false
                    }
                ) {
                    Text("Clear All", color = CrimsonError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Credential Security") },
            text = {
                Text(
                    "• Chat history is stored strictly locally in your device Room SQLite database.\n" +
                        "• Custom API credentials entered in Settings are encrypted with AES-256-GCM via AndroidKeyStore.\n" +
                        "• PDF documents are extracted locally on-device and only sent to AI after explicit user consent.\n" +
                        "• Microphone audio is used only when you activate Voice Mode."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Got It") }
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    val tokens = LocalAiThemeTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tokens.glowSecondary)
        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}
