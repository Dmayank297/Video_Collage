package com.example.videocollage.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.videocollage.data.ml.DbscanClusterer
import com.example.videocollage.data.ml.FaceDetector
import com.example.videocollage.data.ml.MobileFaceNetEmbedder
import com.example.videocollage.data.repository.VideoProcessingRepositoryImpl
import com.example.videocollage.domain.AppearanceTracker
import com.example.videocollage.domain.DetectionGate
import com.example.videocollage.domain.FrameQualityScorer
import com.example.videocollage.domain.VideoProcessor

class VideoCollageViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val faceDetector = FaceDetector()
        val embedder = MobileFaceNetEmbedder(context)
        val dbscanClusterer = DbscanClusterer()
        val appearanceTracker = AppearanceTracker()
        val detectionGate = DetectionGate()
        val frameQualityScorer = FrameQualityScorer()

        val videoProcessor = VideoProcessor(
            context = context.applicationContext,
            faceDetector = faceDetector,
            embedder = embedder,
            dbscanClusterer = dbscanClusterer,
            appearanceTracker = appearanceTracker,
            detectionGate = detectionGate,
            frameQualityScorer = frameQualityScorer
        )

        val repository = VideoProcessingRepositoryImpl(videoProcessor)

        @Suppress("UNCHECKED_CAST")
        return VideoCollageViewModel(repository) as T
    }
}