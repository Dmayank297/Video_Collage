package com.example.videocollage.data.ml

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

class FaceDetector {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.1f)
            .build()
    )

    suspend fun detectFaces(bitmap: Bitmap): List<Face> {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        return detector.process(inputImage).await()
    }

    fun cropFace(sourceBitmap: Bitmap, box: Rect, marginFactor: Float = 0.15f): Bitmap {
        val marginX = (box.width() * marginFactor).toInt()
        val marginY = (box.height() * marginFactor).toInt()

        val left = (box.left - marginX).coerceAtLeast(0)
        val top = (box.top - marginY).coerceAtLeast(0)
        val right = (box.right + marginX).coerceAtMost(sourceBitmap.width)
        val bottom = (box.bottom + marginY).coerceAtMost(sourceBitmap.height)

        return Bitmap.createBitmap(sourceBitmap, left, top, right - left, bottom - top)
    }

    fun close() = detector.close()
}