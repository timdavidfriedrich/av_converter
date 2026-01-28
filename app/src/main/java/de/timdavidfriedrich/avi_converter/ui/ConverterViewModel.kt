package de.timdavidfriedrich.avi_converter.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.timdavidfriedrich.avi_converter.domain.entities.ConverterPresets
import de.timdavidfriedrich.avi_converter.domain.services.ConversionService
import de.timdavidfriedrich.avi_converter.domain.services.ConversionStatus
import de.timdavidfriedrich.avi_converter.domain.services.FileService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ConverterUiState {
    object Idle : ConverterUiState
    data class Loading(
        val progress: Float,
        val currentFileIndex: Int,
        val totalFiles: Int,
    ) : ConverterUiState

    data class Success(val count: Int) : ConverterUiState
    data class Error(val message: String) : ConverterUiState
}

class ConverterViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ConverterUiState>(ConverterUiState.Idle)
    val uiState: StateFlow<ConverterUiState> = _uiState

    val activeConfig = ConverterPresets.CoolpixL25HighQuality

    fun processVideos(sourceUris: List<Uri>) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            var successCount = 0

            sourceUris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            for ((index, uri) in sourceUris.withIndex()) {
                _uiState.value = ConverterUiState.Loading(
                    progress = 0f,
                    currentFileIndex = index + 1,
                    totalFiles = sourceUris.size,
                )

                try {
                    val tempFile = FileService.createTempFile(context.cacheDir, activeConfig)

                    ConversionService.convertVideo(
                        context = context,
                        inputUri = uri,
                        outputFile = tempFile,
                        config = activeConfig,
                    ).collect { status ->
                        when (status) {
                            is ConversionStatus.Progress -> {
                                _uiState.value = ConverterUiState.Loading(
                                    progress = status.percentage,
                                    currentFileIndex = index + 1,
                                    totalFiles = sourceUris.size,
                                )
                            }

                            is ConversionStatus.Completed -> {
                                FileService.saveToGallery(
                                    contentResolver = context.contentResolver,
                                    tempFile = tempFile,
                                    config = activeConfig,
                                )
                                successCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _uiState.value = ConverterUiState.Success(successCount)
        }
    }

    fun resetState() {
        _uiState.value = ConverterUiState.Idle
    }
}