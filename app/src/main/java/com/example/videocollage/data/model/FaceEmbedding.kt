package com.example.videocollage.data.model

data class FaceEmbedding(
    val detectedFace: DetectedFace,
    val vector: FloatArray
)