package de.timdavidfriedrich.av_converter.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import de.timdavidfriedrich.av_converter.R

@UnstableApi
@Composable
fun ConverterScreen(viewModel: ConverterViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.processVideos(uris)
    }

    val inputMimeTypes = viewModel.activeConfig.inputMimeTypesArray

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(R.string.converter_screen_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(32.dp))

            when (val state = uiState) {
                is ConverterUiState.Idle -> {
                    Button(
                        onClick = {
                            filePickerLauncher.launch(inputMimeTypes)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text(stringResource(R.string.converter_screen_select_files_label))
                    }
                }

                is ConverterUiState.LegacyFormatDetected -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.resetState() },
                        title = {
                            Text(
                                stringResource(R.string.converter_screen_legacy_detected_title)
                            )
                        },
                        text = {
                            Text(
                                stringResource(R.string.converter_screen_legacy_detected_message)
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.onConfirmDownloadLegacy() },
                            ) {
                                Text(
                                    stringResource(
                                        R.string.converter_screen_legacy_detected_confirm
                                    )
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.resetState() }) {
                                Text(
                                    stringResource(R.string.converter_screen_legacy_detected_cancel)
                                )
                            }
                        }
                    )
                }

                is ConverterUiState.Loading -> {
                    Text(
                        stringResource(
                            R.string.converter_screen_converting_file_of_label,
                            state.currentFileIndex,
                            state.totalFiles,
                        ),
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    if (state.progress == 0.0f) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (state.progress == 0.0f) {
                        Text(
                            stringResource(R.string.converter_screen_initializing_label)
                        )
                    } else {
                        Text(
                            stringResource(
                                R.string.converter_screen_percentage_label,
                                (state.progress * 100).toInt(),
                            )
                        )
                    }
                }

                is ConverterUiState.DownloadingComponent -> {
                    Text(
                        stringResource(R.string.converter_screen_downloading_component_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(
                            R.string.converter_screen_percentage_label,
                            (state.progress * 100).toInt(),
                        )
                    )
                }

                is ConverterUiState.Success -> {
                    Text(
                        stringResource(R.string.converter_screen_saved_to_gallery),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.resetState() },
                    ) {
                        Text(stringResource(R.string.converter_screen_convert_other_files_label))
                    }
                }

                is ConverterUiState.Error -> {
                    Text(
                        stringResource(R.string.converter_screen_error_message, state.message),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(
                        onClick = { viewModel.resetState() },
                    ) {
                        Text(stringResource(R.string.converter_screen_try_again_label))
                    }
                }
            }
        }
    }
}