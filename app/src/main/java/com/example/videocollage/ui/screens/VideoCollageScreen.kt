package com.example.videocollage.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.videocollage.domain.ProcessingState
import com.example.videocollage.ui.components.ProcessingLoader

@Composable
fun VideoCollageScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: VideoCollageViewModel = viewModel(
        factory = VideoCollageViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState !is CollageUiState.Idle) {
        when (uiState) {
            is CollageUiState.Editor -> viewModel.backToSummary()
            else -> viewModel.reset()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is CollageUiState.Idle -> {
                VideoUploadScreen(onVideoSelected = { viewModel.onVideoSelected(it) })
            }

            is CollageUiState.Processing -> {
                ProcessingLoader(state = ProcessingState.Processing(state.progress, state.stage))
            }

            is CollageUiState.Summary -> {
                AppearanceSummaryScreen(
                    people = state.people,
                    onProceed = { viewModel.proceedToEditor() }
                )
            }

            is CollageUiState.Editor -> {
                CollageEditorScreen(
                    initialSlots = state.slots,
                    onBack = { viewModel.backToSummary() },
                    onCancel = { viewModel.reset() },
                    onExport = {
                        Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            is CollageUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: ${state.message}")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.reset() }) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}