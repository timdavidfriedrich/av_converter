package de.timdavidfriedrich.av_converter.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import de.timdavidfriedrich.av_converter.domain.entities.ConverterPresets
import de.timdavidfriedrich.av_converter.domain.services.ConversionService
import de.timdavidfriedrich.av_converter.domain.services.ConversionStatus
import de.timdavidfriedrich.av_converter.domain.services.FileService
import de.timdavidfriedrich.av_converter.domain.services.LegacyConversionService
import de.timdavidfriedrich.av_converter.domain.services.SmartFormatChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

sealed interface ConverterUiState {
    object Idle : ConverterUiState
    data class Loading(val progress: Float, val currentFileIndex: Int, val totalFiles: Int) : ConverterUiState
    data class LegacyFormatDetected(val reason: String) : ConverterUiState
    data class DownloadingComponent(val progress: Float) : ConverterUiState
    data class Success(val count: Int) : ConverterUiState
    data class Error(val message: String) : ConverterUiState
}

@UnstableApi
class ConverterViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val TAG = ConverterViewModel::class.simpleName
        const val FFMPEG_FEATURE_MODULE = "ffmpeg_feature"
        const val FFMPEG_CONVERSION_SERVICE =
            "de.timdavidfriedrich.av_converter.ffmpeg_feature.FfmpegConversionService"
    }

    private val _uiState = MutableStateFlow<ConverterUiState>(ConverterUiState.Idle)
    val uiState: StateFlow<ConverterUiState> = _uiState
    val activeConfig = ConverterPresets.CoolpixL25HighQuality

    private val splitInstallManager = SplitInstallManagerFactory.create(application)

    private var pendingUris: List<Uri> = emptyList()
    private var pendingIndex: Int = 0

    private var isLegacyBatchMode: Boolean = false

    fun processVideos(sourceUris: List<Uri>) {
        pendingUris = sourceUris
        pendingIndex = 0
        isLegacyBatchMode = false
        continueProcessing()
    }

    private fun continueProcessing() {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext

            if (pendingIndex == 0) {
                pendingUris.forEach {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        _uiState.value = ConverterUiState.Error("Permission denied: ${e.message}")
                        return@launch
                    }
                }
            }

            for (i in pendingIndex until pendingUris.size) {
                val uri = pendingUris[i]
                _uiState.value = ConverterUiState.Loading(0f, i + 1, pendingUris.size)

                if (isLegacyBatchMode) {
                    try {
                        val tempFile = FileService.createTempFile(context.cacheDir, activeConfig)
                        convertLegacy(context, uri, tempFile, i, pendingUris.size)
                    } catch (e: Exception) {
                        _uiState.value = ConverterUiState.Error(
                            "Legacy batch conversion failed: ${e.message}"
                        )
                        return@launch
                    }
                    continue
                }

                try {
                    val tempFile = FileService.createTempFile(context.cacheDir, activeConfig)
                    convertNative(context, uri, tempFile, i, pendingUris.size)
                } catch (e: Exception) {
                    Log.w(TAG, "Native failed: ${e.message}")

                    if (SmartFormatChecker.isLikelySupportedByLegacyPlugin(context, uri)) {

                        if (splitInstallManager.installedModules.contains(FFMPEG_FEATURE_MODULE)) {
                            Log.i(
                                TAG,
                                "Module installed. Switching to Legacy Mode automatically.",
                            )

                            isLegacyBatchMode = true

                            try {
                                val tempFile = FileService.createTempFile(
                                    context.cacheDir,
                                    activeConfig,
                                )
                                convertLegacy(
                                    context,
                                    uri,
                                    tempFile,
                                    i,
                                    pendingUris.size,
                                )
                            } catch (retryEx: Exception) {
                                val errorMessage = "Legacy conversion failed: ${retryEx.message}"
                                _uiState.value = ConverterUiState.Error(errorMessage)
                                Log.e(TAG, errorMessage)
                                return@launch
                            }
                        } else {
                            pendingIndex = i
                            _uiState.value = ConverterUiState.LegacyFormatDetected(
                                "Format not supported natively. Download legacy converter?"
                            )
                            return@launch
                        }
                    } else {
                        _uiState.value = ConverterUiState.Error(
                            "Format not supported: ${e.message}",
                        )
                        return@launch
                    }
                }
            }

            _uiState.value = ConverterUiState.Success(pendingUris.size)
        }
    }

    fun onConfirmDownloadLegacy() {
        viewModelScope.launch {
            if (ensureLegacyModuleInstalled()) {
                isLegacyBatchMode = true
                processCurrentFileWithLegacy()
            } else {
                _uiState.value = ConverterUiState.Error("Download failed.")
            }
        }
    }

    private suspend fun processCurrentFileWithLegacy() {
        val context = getApplication<Application>().applicationContext
        val i = pendingIndex
        val uri = pendingUris[i]

        try {
            _uiState.value = ConverterUiState.Loading(0f, i + 1, pendingUris.size)
            val tempFile = FileService.createTempFile(context.cacheDir, activeConfig)

            convertLegacy(context, uri, tempFile, i, pendingUris.size)

            pendingIndex++
            continueProcessing()

        } catch (e: Exception) {
            val errorMessage = "Legacy conversion failed: ${e.message}"
            _uiState.value = ConverterUiState.Error(errorMessage)
            Log.e(TAG, errorMessage)
        }
    }

    private suspend fun ensureLegacyModuleInstalled(): Boolean {
        if (splitInstallManager.installedModules.contains(FFMPEG_FEATURE_MODULE)) return true

        _uiState.value = ConverterUiState.DownloadingComponent(0.0f)

        return suspendCancellableCoroutine { cont ->
            val request = SplitInstallRequest.newBuilder().addModule(FFMPEG_FEATURE_MODULE).build()
            val listener = { state: com.google.android.play.core.splitinstall.SplitInstallSessionState ->
                if (state.status() == SplitInstallSessionStatus.DOWNLOADING) {
                    val p = state.bytesDownloaded().toFloat() / state.totalBytesToDownload()
                    _uiState.value = ConverterUiState.DownloadingComponent(p)
                }
            }
            val splitInstallStateUpdatedListener = SplitInstallStateUpdatedListener(listener)

            splitInstallManager.registerListener(splitInstallStateUpdatedListener)
            splitInstallManager.startInstall(request)
                .addOnSuccessListener {
                    splitInstallManager.unregisterListener(splitInstallStateUpdatedListener)
                    cont.resume(true)
                }
                .addOnFailureListener {
                    splitInstallManager.unregisterListener(splitInstallStateUpdatedListener)
                    cont.resume(false)
                }
        }
    }

    private suspend fun convertLegacy(
        context: Context,
        uri: Uri,
        outputFile: File,
        index: Int,
        total: Int,
    ) {
        val clazz = Class.forName(FFMPEG_CONVERSION_SERVICE)
        val instance = clazz.getDeclaredConstructor().newInstance() as LegacyConversionService

        instance.convert(context, uri, outputFile, activeConfig).collect { status ->
            handleStatus(status, index, total, context, outputFile)
        }
    }

    private suspend fun convertNative(
        context: Context,
        uri: Uri,
        file: File,
        index: Int,
        total: Int,
    ) {
        ConversionService.convertVideo(context, uri, file, activeConfig).collect { status ->
            handleStatus(status, index, total, context, file)
        }
    }

    private suspend fun handleStatus(
        status: ConversionStatus,
        index: Int,
        total: Int,
        context: Context,
        file: File,
    ) {
        if (status is ConversionStatus.Progress) {
            _uiState.value = ConverterUiState.Loading(status.percentage, index + 1, total)
        } else if (status is ConversionStatus.Completed) {
            FileService.saveToGallery(context.contentResolver, file, activeConfig)
        }
    }

    fun resetState() { _uiState.value = ConverterUiState.Idle }
}