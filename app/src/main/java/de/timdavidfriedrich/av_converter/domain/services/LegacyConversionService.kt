package de.timdavidfriedrich.av_converter.domain.services

import android.content.Context
import android.net.Uri
import de.timdavidfriedrich.av_converter.domain.entities.ConverterConfig
import kotlinx.coroutines.flow.Flow
import java.io.File

interface LegacyConversionService {
    fun convert(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        config: ConverterConfig,
    ): Flow<ConversionStatus>
}