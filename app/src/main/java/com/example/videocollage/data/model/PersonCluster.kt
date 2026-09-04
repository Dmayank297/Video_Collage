package com.example.videocollage.data.model

data class PersonCluster(
    val clusterId: Int,
    val embeddings: List<FaceEmbedding>,
    val appearances: List<FaceAppearance>,
    val bestFrame: DetectedFace?
)