package de.timdavidfriedrich.av_converter.domain.services

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import de.timdavidfriedrich.av_converter.domain.entities.ConverterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

object FileService {

    fun createTempFile(cacheDir: File, config: ConverterConfig): File {
        return File(
            cacheDir,
            "${config.tempFilenamePrefix}${System.currentTimeMillis()}.${config.fileExtension}",
        )
    }

    suspend fun saveToGallery(
        contentResolver: ContentResolver,
        tempFile: File,
        config: ConverterConfig
    ): Uri? = withContext(Dispatchers.IO) {

        val contentValues = ContentValues().apply {
            put(
                MediaStore.Video.Media.DISPLAY_NAME,
                "${config.filenamePrefix}${System.currentTimeMillis()}.${config.fileExtension}",
            )
            put(
                MediaStore.Video.Media.MIME_TYPE,
                config.mediaStoreMimeType,
            )
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/${config.outputDirectoryName}",
            )
            put(
                MediaStore.Video.Media.IS_PENDING,
                1,
            )
        }

        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val itemUri = contentResolver.insert(collection, contentValues)

        try {
            itemUri?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(tempFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)

                return@withContext uri
            }
        } catch (e: Exception) {
            itemUri?.let { contentResolver.delete(it, null, null) }
            e.printStackTrace()
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        return@withContext null
    }
}