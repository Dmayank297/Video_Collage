package com.example.videocollage.domain

import android.graphics.Bitmap
import com.example.videocollage.utils.ImageQualityUtils
import com.google.mlkit.vision.face.Face
import kotlin.math.abs

class DetectionGate(
    private val minFaceRatio: Float = 0.02f,
    private val sharpnessThreshold: Double = 3.0,
    private val maxHeadEulerAngleY: Float = 25f,
    private val maxHeadEulerAngleZ: Float = 20f
) {

    fun passes(face: Face, faceCrop: Bitmap, imageWidth: Int, imageHeight: Int): Boolean {
        val box = face.boundingBox
        val faceRatio = (box.width() * box.height()).toFloat() / (imageWidth * imageHeight)
        val yaw = abs(face.headEulerAngleY)
        val roll = abs(face.headEulerAngleZ)
        val sharpness = ImageQualityUtils.laplacianVariance(faceCrop)

        android.util.Log.d(
            "DetectionGate",
            "faceRatio=$faceRatio (min=$minFaceRatio) | yaw=$yaw (max=$maxHeadEulerAngleY) | " +
                    "roll=$roll (max=$maxHeadEulerAngleZ) | sharpness=$sharpness (min=$sharpnessThreshold)"
        )

        if (faceRatio < minFaceRatio) return false
        if (yaw >= maxHeadEulerAngleY) return false
        if (roll >= maxHeadEulerAngleZ) return false
        return sharpness >= sharpnessThreshold
    }
}