package de.timdavidfriedrich.av_converter.ffmpeg_feature

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import de.timdavidfriedrich.av_converter.domain.entities.ConverterConfig
import de.timdavidfriedrich.av_converter.domain.services.ConversionStatus
import de.timdavidfriedrich.av_converter.domain.services.LegacyConversionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream

class FfmpegConversionService : LegacyConversionService {

    companion object {
        val TAG = FfmpegConversionService::class.simpleName
    }

    override fun convert(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        config: ConverterConfig
    ): Flow<ConversionStatus> = callbackFlow {

        val tempInput = File(
            context.cacheDir,
            "ffmpeg_raw_${System.currentTimeMillis()}",
        )
        try {
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(tempInput).use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        val durationMs = try {
            val mediaInfo = FFprobeKit.getMediaInformation(tempInput.absolutePath)
            val durSec = mediaInfo.mediaInformation.duration.toDouble()
            (durSec * 1000).toLong()
        } catch (e: Exception) {
            Log.w(TAG, "Could not probe duration: ${e.message}")
            0L
        }

        val command = StringBuilder().apply {
            append("-y -i \"${tempInput.absolutePath}\" ")
            append("-c:v ${getVideoCodecName(config.videoCodecMimeType)} ")
            append("-preset ${config.videoPreset} ")
            append("-tune ${config.videoTune} ")
            append("-pix_fmt ${config.pixelFormat} ")
            if (config.targetResolution.first != -1) {
                append("-vf \"scale=${config.targetResolution.first}:${config.targetResolution.second}\" ")
            }
            if (config.crf != null) {
                append("-crf ${config.crf} ")
            } else {
                append("-b:v ${config.videoBitrate} -maxrate ${config.videoBitrate} -bufsize ${config.videoBitrate * 2} ")
            }
            if (config.targetFps > 0.0) {
                append("-r ${config.targetFps} ")
            }
            append("-force_key_frames \"expr:gte(t,n_forced*${config.keyframeInterval})\" ")
            append("-c:a ${getAudioCodecName(config.audioCodecMimeType)} -b:a ${config.audioBitrate} ")
            if (config.audioSampleRate > 0) {
                append("-ar ${config.audioSampleRate} ")
            }
            append("\"${outputFile.absolutePath}\"")
        }.toString()

        Log.d(TAG, "Run: $command")

        val session = FFmpegKit.executeAsync(command,
            { session ->
                if (tempInput.exists()) tempInput.delete()

                if (ReturnCode.isSuccess(session.returnCode)) {
                    trySend(ConversionStatus.Completed(outputFile.absolutePath))
                    close()
                } else {
                    val logs = session.logs.joinToString("\n") { it.message }
                    close(Exception("FFmpeg Failed (RC ${session.returnCode}): $logs"))
                }
            },
            { },
            { stats ->
                if (durationMs > 0) {
                    val progress = (stats.time.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    trySend(ConversionStatus.Progress(progress))
                } else {
                    trySend(ConversionStatus.Progress(0.1f))
                }
            }
        )

        awaitClose { session.cancel() }
    }.flowOn(Dispatchers.IO)

    @OptIn(UnstableApi::class)
    private fun getVideoCodecName(mimeType: String): String {
        return when (mimeType) {
            MimeTypes.VIDEO_H264 -> "libx264"
            MimeTypes.VIDEO_H265 -> "libx265"
            MimeTypes.VIDEO_VP9 -> "libvpx-vp9"
            MimeTypes.VIDEO_AV1 -> "libaom-av1"
            else -> "mpeg4"
        }
    }

    private fun getAudioCodecName(mimeType: String): String {
        return when (mimeType) {
            MimeTypes.AUDIO_AAC -> "aac"
            MimeTypes.AUDIO_MPEG -> "libmp3lame"
            MimeTypes.AUDIO_VORBIS -> "libvorbis"
            MimeTypes.AUDIO_OPUS -> "libopus"
            MimeTypes.AUDIO_FLAC -> "flac"
            else -> "copy"
        }
    }
}