package de.timdavidfriedrich.av_converter.domain.entities

import android.media.MediaCodecInfo
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

data class ConverterConfig(
    val inputMimeTypes: List<String>,
    val videoCodecMimeType: String,
    val audioCodecMimeType: String,
    val videoBitrate: Int,
    val bitrateMode: Int,
    val profile: Int,
    val level: Int,
    val keyframeInterval: Float,
    val fileExtension: String,
    val mediaStoreMimeType: String,
    val outputDirectoryName: String,
    val filenamePrefix: String,
    val audioBitrate: Int,
    val videoPreset: String,
    val pixelFormat: String,
    val videoTune: String,
    val targetResolution: Pair<Int, Int>,
    val targetFps: Double,
    val audioSampleRate: Int,
    val crf: Int? = null,
    val tempFilenamePrefix: String = "processing_",
) {
    val inputMimeTypesArray: Array<String> = inputMimeTypes.toTypedArray()
}


enum class InputFormatGroup(val formats: List<String>) {
    LEGACY_AVI(listOf("video/avi", "video/x-msvideo", "video/mj2", "video/*")),
    MODERN_ALL(listOf("video/mp4", "video/x-matroska", "video/webm")),
}

enum class OutputExtension(val value: String) {
    MP4("mp4"),
    MKV("mkv"),
    WEBM("webm"),
}

@UnstableApi
enum class OutputMimeType(val value: String) {
    VIDEO_MP4(MimeTypes.VIDEO_MP4),
    VIDEO_WEBM(MimeTypes.VIDEO_WEBM),
    VIDEO_MATROSKA(MimeTypes.VIDEO_MATROSKA),
}

@UnstableApi
enum class VideoCodec(val mimeType: String) {
    H264(MimeTypes.VIDEO_H264),
    H265(MimeTypes.VIDEO_H265),
    VP9(MimeTypes.VIDEO_VP9),
    AV1(MimeTypes.VIDEO_AV1),
}

enum class AudioCodec(val mimeType: String) {
    AAC(MimeTypes.AUDIO_AAC),
    MP3(MimeTypes.AUDIO_MPEG),
    FLAC(MimeTypes.AUDIO_FLAC),
    OPUS(MimeTypes.AUDIO_OPUS),
}

enum class FFmpegPreset(val value: String) {
    ULTRAFAST("ultrafast"),
    SUPERFAST("superfast"),
    VERY_FAST("veryfast"),
    FASTER("faster"),
    FAST("fast"),
    MEDIUM("medium"),
    SLOW("slow"),
    SLOWER("slower"),
    VERY_SLOW("veryslow"),
}

enum class FFmpegPixelFormat(val value: String) {
    YUV420P("yuv420p"),
    YUV422P("yuv422p"),
    YUV444P("yuv444p"),
    RGB24("rgb24"),
}

enum class BitrateMode(val value: Int) {
    CQ(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ),
    VBR(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR),
    CBR(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR),
}

enum class AvcProfile(val value: Int) {
    BASELINE(MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline),
    MAIN(MediaCodecInfo.CodecProfileLevel.AVCProfileMain),
    HIGH(MediaCodecInfo.CodecProfileLevel.AVCProfileHigh),
}

enum class AvcLevel(val value: Int) {
    L3(MediaCodecInfo.CodecProfileLevel.AVCLevel3),
    L31(MediaCodecInfo.CodecProfileLevel.AVCLevel31),
    L4(MediaCodecInfo.CodecProfileLevel.AVCLevel4),
    L41(MediaCodecInfo.CodecProfileLevel.AVCLevel41),
    L5(MediaCodecInfo.CodecProfileLevel.AVCLevel5),
}

enum class VideoBitrate(val value: Int) {
    MBPS_50(50_000_000), // Extreme High Quality
    MBPS_30(30_000_000), // Coolpix L25 High Quality
    MBPS_20(20_000_000), // Standard 1080p
    MBPS_10(10_000_000), // High 720p
    MBPS_5(5_000_000),   // Standard 720p
}

enum class AudioBitrate(val value: Int) {
    KBPS_320(320_000),
    KBPS_192(192_000),
    KBPS_128(128_000),
    KBPS_96(96_000),
}

enum class KeyframeInterval(val value: Float) {
    HALF_SECOND(0.5f),
    ONE_SECOND(1.0f),
    TWO_SECONDS(2.0f),
    FIVE_SECONDS(5.0f),
}

enum class LosslessTuning(val crf: Int) {
    LOSSLESS(0),
    NEAR_LOSSLESS(12),
    VISUALLY_TRANSPARENT(18),
    HIGH_QUALITY(23),
}

enum class VideoTune(val value: String) {
    NONE("none"),
    FILM("film"),
    ANIMATION("animation"),
    GRAIN("grain"),
    STILLIMAGE("stillimage"),
    FAST_DECODE("fastdecode"),
}

enum class TargetResolution(val width: Int, val height: Int) {
    ORIGINAL(-1, -1),
    SD_480P(640, 480),
    HD_720P(1280, 720),
    FHD_1080P(1920, 1080),
}

enum class TargetFrameRate(val value: Double) {
    MATCH_SOURCE(0.0),
    FPS_15(15.0),
    FPS_24(23.976),
    FPS_30(29.97),
    FPS_60(60.0),
}

enum class AudioSampleRate(val value: Int) {
    ORIGINAL(0),
    HZ_22050(22050),
    HZ_32000(32000),
    HZ_44100(44100),
    HZ_48000(48000),
}


@UnstableApi
object ConverterPresets {

    val CoolpixL25HighQuality = ConverterConfig(
        inputMimeTypes = InputFormatGroup.LEGACY_AVI.formats,
        videoCodecMimeType = VideoCodec.H264.mimeType,
        audioCodecMimeType = AudioCodec.AAC.mimeType,
        videoBitrate = VideoBitrate.MBPS_30.value,
        bitrateMode = BitrateMode.VBR.value,
        profile = AvcProfile.HIGH.value,
        level = AvcLevel.L41.value,
        keyframeInterval = KeyframeInterval.ONE_SECOND.value,
        fileExtension = OutputExtension.MP4.value,
        mediaStoreMimeType = OutputMimeType.VIDEO_MP4.value,
        outputDirectoryName = "CoolpixExports",
        filenamePrefix = "Coolpix_L25_",
        audioBitrate = AudioBitrate.KBPS_192.value,
        videoPreset = FFmpegPreset.VERY_SLOW.value,
        pixelFormat = FFmpegPixelFormat.YUV420P.value,
        videoTune = VideoTune.STILLIMAGE.value,
        targetResolution = TargetResolution.ORIGINAL.width to TargetResolution.ORIGINAL.height,
        targetFps = TargetFrameRate.MATCH_SOURCE.value,
        audioSampleRate = AudioSampleRate.HZ_48000.value,
        crf = LosslessTuning.NEAR_LOSSLESS.crf,
    )
}