package com.example.videocollage.data.model

import android.graphics.Bitmap
import android.graphics.Rect

data class DetectedFace(
    val faceCrop: Bitmap,
    val boundingBox: Rect,
    val sourceFrameWidth: Int,
    val sourceFrameHeight: Int,
    val timestampMs: Long,
    val headEulerAngleX: Float,
    val headEulerAngleY: Float,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val smilingProbability: Float?
)