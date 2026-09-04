package com.example.videocollage.domain

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.videocollage.utils.ImageQualityUtils
import kotlin.math.abs

data class FrameCandidate(
    val bitmap: Bitmap,
    val faceBox: Rect,
    val frameWidth: Int,
    val frameHeight: Int,
    val headEulerAngleX: Float,
    val headEulerAngleY: Float,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val smilingProbability: Float?
)

class FrameQualityScorer {

    fun score(candidate: FrameCandidate): Double {
        val frontality = frontalityScore(candidate.headEulerAngleX, candidate.headEulerAngleY)
        val sharpness = sharpnessScore(candidate.bitmap)
        val eyeOpenness = eyeOpennessScore(candidate.leftEyeOpenProbability, candidate.rightEyeOpenProbability)
        val smile = candidate.smilingProbability?.toDouble() ?: 0.5
        val visibility = visibilityScore(candidate.faceBox, candidate.frameWidth, candidate.frameHeight)

        return (0.30 * frontality) +
                (0.25 * sharpness) +
                (0.20 * eyeOpenness) +
                (0.15 * smile) +
                (0.10 * visibility)
    }

    fun bestFrame(candidates: List<FrameCandidate>): FrameCandidate? {
        return candidates.maxByOrNull { score(it) }
    }

    private fun frontalityScore(angleX: Float, angleY: Float): Double {
        val maxAngle = 45.0
        val deviation = (abs(angleX) + abs(angleY)) / 2.0
        return (1.0 - (deviation / maxAngle)).coerceIn(0.0, 1.0)
    }

    private fun sharpnessScore(bitmap: Bitmap): Double {
        val variance = ImageQualityUtils.laplacianVariance(bitmap)
        val maxExpectedVariance = 500.0
        return (variance / maxExpectedVariance).coerceIn(0.0, 1.0)
    }

    private fun eyeOpennessScore(left: Float?, right: Float?): Double {
        if (left == null || right == null) return 0.5
        return ((left + right) / 2.0).coerceIn(0.0, 1.0)
    }

    private fun visibilityScore(faceBox: Rect, frameWidth: Int, frameHeight: Int): Double {
        val clippedLeft = faceBox.left <= 0
        val clippedTop = faceBox.top <= 0
        val clippedRight = faceBox.right >= frameWidth
        val clippedBottom = faceBox.bottom >= frameHeight

        val clippedEdges = listOf(clippedLeft, clippedTop, clippedRight, clippedBottom).count { it }
        return (1.0 - (clippedEdges * 0.25)).coerceIn(0.0, 1.0)
    }
}