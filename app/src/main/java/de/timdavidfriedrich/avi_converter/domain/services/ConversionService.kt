package de.timdavidfriedrich.avi_converter.domain.services

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import de.timdavidfriedrich.avi_converter.domain.entities.ConverterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

sealed interface ConversionStatus {
    data class Progress(val percentage: Float) : ConversionStatus
    data class Completed(val outputPath: String) : ConversionStatus
}

object ConversionService {

    @OptIn(UnstableApi::class)
    fun convertVideo(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        config: ConverterConfig,
    ): Flow<ConversionStatus> = callbackFlow {

        val videoSettings = VideoEncoderSettings.Builder()
            .setBitrate(config.bitrate)
            .setBitrateMode(config.bitrateMode)
            .setEncodingProfileLevel(config.profile, config.level)
            .setiFrameIntervalSeconds(config.iFrameIntervalSeconds)
            .build()

        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(videoSettings)
            .build()

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                trySend(ConversionStatus.Completed(outputFile.absolutePath))
                close()
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException,
            ) {
                close(exportException)
            }
        }

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(config.videoCodecMimeType)
            .setAudioMimeType(config.audioCodecMimeType)
            .setEncoderFactory(encoderFactory)
            .addListener(listener)
            .build()

        transformer.start(MediaItem.fromUri(inputUri), outputFile.absolutePath)

        val progressHolder = ProgressHolder()
        val progressJob = launch {
            while (isActive) {
                val progressState = transformer.getProgress(progressHolder)
                if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                    trySend(ConversionStatus.Progress(progressHolder.progress / 100f))
                }
                delay(200)
            }
        }

        awaitClose {
            progressJob.cancel()
            transformer.cancel()
        }
    }.flowOn(Dispatchers.Main)
}