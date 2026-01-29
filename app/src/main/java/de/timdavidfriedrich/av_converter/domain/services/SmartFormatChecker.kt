package de.timdavidfriedrich.av_converter.domain.services

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.util.Locale

object SmartFormatChecker {

    private val LEGACY_EXTENSIONS = setOf(
        "avi", "mkv", "flv", "wmv", "mov", "vob", "mpg", "mpeg",
        "3gp", "asf", "divx", "m2ts", "mts", "ts", "webm", "ogv"
    )

    fun isLikelySupportedByLegacyPlugin(context: Context, uri: Uri): Boolean {
        val extension = getExtension(context, uri)?.lowercase(Locale.ROOT)
        if (extension != null && LEGACY_EXTENSIONS.contains(extension)) {
            return true
        }

        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != null) {
            if (mimeType.startsWith("video/") || mimeType.startsWith("audio/")) {
                return true
            }
        }

        return false
    }

    private fun getExtension(context: Context, uri: Uri): String? {
        return if (uri.scheme == "content") {
            val mime = context.contentResolver.getType(uri)
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        } else {
            MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        }
    }
}