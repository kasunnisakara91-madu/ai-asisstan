package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.AiService
import com.example.ai.ConfigurationRequiredException
import com.example.data.local.preferences.ImageGenProvider
import com.example.data.local.preferences.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed interface ImageGenResultState {
    data object Idle : ImageGenResultState
    data object Loading : ImageGenResultState
    data class Success(val bitmap: Bitmap, val prompt: String) : ImageGenResultState
    data class Error(val message: String, val requiresConfiguration: Boolean) : ImageGenResultState
}

data class ImageGeneratorUiState(
    val prompt: String = "",
    val selectedStyle: String = "Futuristic Neon",
    val provider: ImageGenProvider = ImageGenProvider.FIREBASE_NANO_BANANA,
    val result: ImageGenResultState = ImageGenResultState.Idle,
    val actionFeedback: String? = null
)

class ImageGeneratorViewModel(
    private val aiService: AiService,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _prompt = MutableStateFlow("")
    private val _selectedStyle = MutableStateFlow("Futuristic Neon")
    private val _result = MutableStateFlow<ImageGenResultState>(ImageGenResultState.Idle)
    private val _feedback = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ImageGeneratorUiState> = combine(
        _prompt,
        _selectedStyle,
        settingsRepository.settingsFlow,
        _result,
        _feedback
    ) { prompt, style, settings, result, feedback ->
        ImageGeneratorUiState(
            prompt = prompt,
            selectedStyle = style,
            provider = settings.imageGenProvider,
            result = result,
            actionFeedback = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ImageGeneratorUiState()
    )

    fun updatePrompt(value: String) {
        _prompt.value = value
    }

    fun selectStyle(style: String) {
        _selectedStyle.value = style
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    fun generateImage() {
        val raw = _prompt.value.trim()
        if (raw.isEmpty() || _result.value is ImageGenResultState.Loading) return

        val styleSuffix = if (_selectedStyle.value == "Natural") "" else ", ${_selectedStyle.value} style"
        val fullPrompt = "$raw$styleSuffix"

        viewModelScope.launch {
            _feedback.value = null
            _result.value = ImageGenResultState.Loading
            try {
                val bitmap = aiService.generateImage(fullPrompt)
                _result.value = ImageGenResultState.Success(bitmap = bitmap, prompt = raw)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ConfigurationRequiredException) {
                _result.value = ImageGenResultState.Error(
                    message = e.message ?: "Image provider configuration required.",
                    requiresConfiguration = true
                )
            } catch (e: Exception) {
                val msg = AiService.formatUserError(e)
                _result.value = ImageGenResultState.Error(
                    message = msg,
                    requiresConfiguration = msg.contains("Settings", ignoreCase = true) ||
                        msg.contains("Blaze", ignoreCase = true) ||
                        msg.contains("API", ignoreCase = true)
                )
            }
        }
    }

    fun saveImage(context: Context, bitmap: Bitmap, fallbackUri: Uri? = null) {
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val fileName = "AI_ASISSTANT_${System.currentTimeMillis()}.png"
                    if (fallbackUri != null) {
                        context.contentResolver.openOutputStream(fallbackUri)?.use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        } ?: throw IllegalStateException("Could not write to selected location.")
                        "Image saved successfully."
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                            put(
                                MediaStore.Images.Media.RELATIVE_PATH,
                                "${Environment.DIRECTORY_PICTURES}/AI_ASISSTANT"
                            )
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                        val resolver = context.contentResolver
                        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                            ?: throw IllegalStateException("MediaStore failed to allocate image slot.")
                        resolver.openOutputStream(uri)?.use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                        "Saved to Pictures/AI_ASISSTANT"
                    } else {
                        val dir = File(
                            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            "AI_ASISSTANT"
                        ).apply { mkdirs() }
                        val file = File(dir, fileName)
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        "Saved to app Pictures folder"
                    }
                }
            }
            _feedback.value = outcome.fold(
                onSuccess = { it },
                onFailure = { it.localizedMessage ?: "Failed to save image." }
            )
        }
    }

    suspend fun prepareShareIntent(context: Context, bitmap: Bitmap): Intent? =
        withContext(Dispatchers.IO) {
            runCatching {
                val shareDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
                val outFile = File(shareDir, "ai_assisstant_share_${System.currentTimeMillis()}.png")
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    outFile
                )
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }.getOrNull()
        }

    companion object {
        fun provideFactory(
            aiService: AiService,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ImageGeneratorViewModel(aiService, settingsRepository) }
        }
    }
}
