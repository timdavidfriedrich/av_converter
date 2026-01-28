package de.timdavidfriedrich.avi_converter.domain.entities

import android.media.MediaCodecInfo
import androidx.media3.common.MimeTypes
import kotlin.String

data class ConverterConfig(
    val inputMimeTypes: List<String>,
    val videoCodecMimeType: String,
    val audioCodecMimeType: String,
    val bitrate: Int,
    val bitrateMode: Int,
    val profile: Int,
    val level: Int,
    val iFrameIntervalSeconds: Float,
    val fileExtension: String,
    val mediaStoreMimeType: String,
    val outputDirectoryName: String,
    val filenamePrefix: String,
    val tempFilenamePrefix: String = "processing_"
) {
    val inputMimeTypesArray: Array<String> = inputMimeTypes.toTypedArray()
}

object ConverterPresets {
    val CoolpixL25HighQuality = ConverterConfig(
        inputMimeTypes = listOf("video/avi", "video/x-msvideo", "video/mj2", "video/*"),
        videoCodecMimeType = MimeTypes.VIDEO_H264,
        audioCodecMimeType = MimeTypes.AUDIO_AAC,
        bitrate = 30_000_000,
        bitrateMode = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR,
        profile = MediaCodecInfo.CodecProfileLevel.AVCProfileHigh,
        level = MediaCodecInfo.CodecProfileLevel.AVCLevel41,
        iFrameIntervalSeconds = 1.0f,
        fileExtension = "mp4",
        mediaStoreMimeType = "video/mp4",
        outputDirectoryName = "CoolpixExports",
        filenamePrefix = "Coolpix_L25_"
    )
}