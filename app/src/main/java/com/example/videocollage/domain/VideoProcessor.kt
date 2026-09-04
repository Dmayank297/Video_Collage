package com.example.videocollage.domain

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.videocollage.data.ml.DbscanClusterer
import com.example.videocollage.data.ml.FaceDetector
import com.example.videocollage.data.ml.MobileFaceNetEmbedder
import com.example.videocollage.data.model.DetectedFace
import com.example.videocollage.data.model.FaceEmbedding
import com.example.videocollage.data.model.PersonResult
import com.example.videocollage.data.model.ProcessingResult
import com.example.videocollage.data.model.VideoFrame

sealed class ProcessingState {
    object Idle : ProcessingState()
    data class Processing(val progress: Float, val stage: String) : ProcessingState()
    data class Success(val result: ProcessingResult) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

class VideoProcessor(
    private val context: Context,
    private val faceDetector: FaceDetector,
    private val embedder: MobileFaceNetEmbedder,
    private val dbscanClusterer: DbscanClusterer,
    private val appearanceTracker: AppearanceTracker,
    private val detectionGate: DetectionGate,
    private val frameQualityScorer: FrameQualityScorer,
    private val sampleIntervalMs: Long = 500L
) {

    suspend fun process(
        videoUri: Uri,
        onProgress: (ProcessingState.Processing) -> Unit
    ): ProcessingResult {
        onProgress(ProcessingState.Processing(0f, "Extracting frames"))
        val frames = extractFrames(videoUri)
        android.util.Log.d("VideoProcessor", "Extracted ${frames.size} frames")

        val faceEmbeddings = mutableListOf<FaceEmbedding>()

        frames.forEachIndexed { index, frame ->
            onProgress(
                ProcessingState.Processing(
                    progress = index.toFloat() / frames.size,
                    stage = "Detecting faces (${index + 1}/${frames.size})"
                )
            )

            val faces = faceDetector.detectFaces(frame.bitmap)
            android.util.Log.d("VideoProcessor", "Frame @${frame.timestampMs}ms: ${faces.size} faces detected")

            for (face in faces) {
                val faceCrop = faceDetector.cropFace(frame.bitmap, face.boundingBox)

                val gatePasses = detectionGate.passes(
                    face = face,
                    faceCrop = faceCrop,
                    imageWidth = frame.bitmap.width,
                    imageHeight = frame.bitmap.height
                )
                android.util.Log.d("VideoProcessor", "  gate passed=$gatePasses")
                if (!gatePasses) continue

                val detectedFace = DetectedFace(
                    faceCrop = faceCrop,
                    boundingBox = face.boundingBox,
                    sourceFrameWidth = frame.bitmap.width,
                    sourceFrameHeight = frame.bitmap.height,
                    timestampMs = frame.timestampMs,
                    headEulerAngleX = face.headEulerAngleX,
                    headEulerAngleY = face.headEulerAngleY,
                    leftEyeOpenProbability = face.leftEyeOpenProbability,
                    rightEyeOpenProbability = face.rightEyeOpenProbability,
                    smilingProbability = face.smilingProbability
                )

                val embedding = embedder.getEmbedding(faceCrop)
                faceEmbeddings.add(FaceEmbedding(detectedFace, embedding))
            }
        }

        onProgress(ProcessingState.Processing(0.7f, "Clustering people"))
        faceEmbeddings.forEachIndexed { i, fe ->
            val sec = fe.detectedFace.timestampMs / 1000.0
            android.util.Log.d("VideoProcessor", "Detection #$i -> timestamp: ${fe.detectedFace.timestampMs}ms (${"%.2f".format(sec)}s)")
        }

        val vectors = faceEmbeddings.map { it.vector }
        val labels = dbscanClusterer.cluster(vectors)

        val timedDetections = mutableListOf<TimedDetection>()
        val clusteredEmbeddings = mutableMapOf<Int, MutableList<FaceEmbedding>>()

        faceEmbeddings.forEachIndexed { i, faceEmbedding ->
            val clusterId = labels[i]
            if (clusterId != -1) {
                timedDetections.add(TimedDetection(clusterId, faceEmbedding.detectedFace.timestampMs))
                clusteredEmbeddings.getOrPut(clusterId) { mutableListOf() }.add(faceEmbedding)
            }
        }

        clusteredEmbeddings.forEach { (clusterId, embeddings) ->
            val timestamps = embeddings.map { "${it.detectedFace.timestampMs / 1000.0}s" }
            android.util.Log.d("VideoProcessor", "Cluster $clusterId member count: ${embeddings.size}, timestamps: $timestamps")
        }

        onProgress(ProcessingState.Processing(0.85f, "Tracking appearances"))
        val appearances = appearanceTracker.track(timedDetections)
        val appearanceCounts = appearances.groupingBy { it.clusterId }.eachCount()

        appearances.groupBy { it.clusterId }.forEach { (clusterId, apps) ->
            val windows = apps.map { "${it.startMs / 1000.0}s - ${it.endMs / 1000.0}s" }
            android.util.Log.d("VideoProcessor", "Cluster $clusterId Appearances (${apps.size}x): $windows")
        }

        onProgress(ProcessingState.Processing(0.95f, "Selecting best frames"))

        val people = clusteredEmbeddings.mapNotNull { (clusterId, embeddingsForCluster) ->
            val candidateToFace = embeddingsForCluster.associateBy { it.detectedFace.toFrameCandidate() }
            val best = frameQualityScorer.bestFrame(candidateToFace.keys.toList()) ?: return@mapNotNull null
            val bestDetectedFace = candidateToFace.getValue(best).detectedFace

            PersonResult(
                clusterId = clusterId,
                appearanceCount = appearanceCounts[clusterId] ?: 0,
                bestFrame = bestDetectedFace
            )
        }

        return ProcessingResult(people)
    }

    private fun DetectedFace.toFrameCandidate() = FrameCandidate(
        bitmap = faceCrop,
        faceBox = boundingBox,
        frameWidth = sourceFrameWidth,
        frameHeight = sourceFrameHeight,
        headEulerAngleX = headEulerAngleX,
        headEulerAngleY = headEulerAngleY,
        leftEyeOpenProbability = leftEyeOpenProbability,
        rightEyeOpenProbability = rightEyeOpenProbability,
        smilingProbability = smilingProbability
    )

    private fun extractFrames(videoUri: Uri): List<VideoFrame> {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)

        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L

        val frames = mutableListOf<VideoFrame>()
        var timeMs = 0L

        while (timeMs < durationMs) {
            val bitmap: Bitmap? = retriever.getFrameAtTime(
                timeMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST
            )
            if (bitmap != null) {
                frames.add(VideoFrame(bitmap, timeMs))
            }
            timeMs += sampleIntervalMs
        }

        retriever.release()
        return frames
    }
}