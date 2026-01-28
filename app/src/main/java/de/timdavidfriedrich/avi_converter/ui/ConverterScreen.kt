package de.timdavidfriedrich.avi_converter.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.timdavidfriedrich.avi_converter.R

@Composable
fun ConverterScreen(
    viewModel: ConverterViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.processVideos(uris)
        }
    }

    val inputMimeTypes = viewModel.activeConfig.inputMimeTypesArray

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
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
                            filePickerLauncher.launch(input = inputMimeTypes)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text(stringResource(R.string.convert_screen_select_files_label))
                    }
                }

                is ConverterUiState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(
                                R.string.converter_screen_converting_file_of_label,
                                state.currentFileIndex,
                                state.totalFiles,
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${(state.progress * 100).toInt()}%")
                    }
                }

                is ConverterUiState.Success -> {
                    Text(
                        stringResource(R.string.converter_screen_saved_to_gallery_label),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.resetState() }) {
                        Text(stringResource(R.string.converter_screen_convert_other_files_label))
                    }
                }

                is ConverterUiState.Error -> {
                    Text(
                        stringResource(R.string.converter_screen_error_label, state.message),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.resetState() }) {
                        Text(stringResource(R.string.converter_screen_try_again_label))
                    }
                }
            }
        }
    }
}