package com.example.videocollage.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videocollage.data.model.PersonResult
import com.example.videocollage.data.repository.VideoProcessingRepository
import com.example.videocollage.domain.CollageSlot
import com.example.videocollage.domain.ProcessingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CollageUiState {
    object Idle : CollageUiState
    data class Processing(val progress: Float, val stage: String) : CollageUiState
    data class Summary(val people: List<PersonResult>) : CollageUiState
    data class Editor(
        val people: List<PersonResult>,
        val slots: List<CollageSlot>
    ) : CollageUiState
    data class Error(val message: String) : CollageUiState
}

class VideoCollageViewModel(
    private val repository: VideoProcessingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CollageUiState>(CollageUiState.Idle)
    val uiState: StateFlow<CollageUiState> = _uiState.asStateFlow()

    private var currentPeople: List<PersonResult> = emptyList()

    fun onVideoSelected(videoUri: Uri) {
        viewModelScope.launch {
            repository.processVideo(videoUri).collect { state ->
                when (state) {
                    is ProcessingState.Idle -> _uiState.value = CollageUiState.Idle
                    is ProcessingState.Processing -> _uiState.value = CollageUiState.Processing(state.progress, state.stage)
                    is ProcessingState.Success -> {
                        val sorted = state.result.people
                            .sortedByDescending { it.appearanceCount }
                            .take(8)
                        currentPeople = sorted
                        if (sorted.isNotEmpty()) {
                            _uiState.value = CollageUiState.Summary(sorted)
                        } else {
                            _uiState.value = CollageUiState.Error("No faces detected in the video")
                        }
                    }
                    is ProcessingState.Error -> _uiState.value = CollageUiState.Error(state.message)
                }
            }
        }
    }

    fun proceedToEditor() {
        val people = currentPeople
        if (people.isNotEmpty()) {
            val slots = people.map { person ->
                CollageSlot(
                    personId = person.clusterId,
                    bitmap = person.bestFrame.faceCrop
                )
            }
            _uiState.value = CollageUiState.Editor(people, slots)
        }
    }

    fun backToSummary() {
        if (currentPeople.isNotEmpty()) {
            _uiState.value = CollageUiState.Summary(currentPeople)
        } else {
            reset()
        }
    }

    fun reset() {
        currentPeople = emptyList()
        _uiState.value = CollageUiState.Idle
    }
}