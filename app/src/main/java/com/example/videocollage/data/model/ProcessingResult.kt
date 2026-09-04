package com.example.videocollage.data.model

data class PersonResult(
    val clusterId: Int,
    val appearanceCount: Int,
    val bestFrame: DetectedFace
)

data class ProcessingResult(
    val people: List<PersonResult>
)