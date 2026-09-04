package com.example.videocollage.data.repository

import android.net.Uri
import com.example.videocollage.domain.ProcessingState
import kotlinx.coroutines.flow.Flow

interface VideoProcessingRepository {
    fun processVideo(videoUri: Uri): Flow<ProcessingState>
}