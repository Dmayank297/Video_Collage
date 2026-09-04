package com.example.videocollage.data.repository

import android.net.Uri
import com.example.videocollage.domain.ProcessingState
import com.example.videocollage.domain.VideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

class VideoProcessingRepositoryImpl(
    private val videoProcessor: VideoProcessor
) : VideoProcessingRepository {

    override fun processVideo(videoUri: Uri): Flow<ProcessingState> = callbackFlow {
        trySend(ProcessingState.Processing(0f, "Starting"))

        try {
            val result = videoProcessor.process(videoUri) { processing ->
                trySend(processing)
            }
            trySend(ProcessingState.Success(result))
        } catch (e: Exception) {
            trySend(ProcessingState.Error(e.message ?: "Unknown error during processing"))
        }

        close()
        awaitClose { }
    }.flowOn(Dispatchers.Default)
}