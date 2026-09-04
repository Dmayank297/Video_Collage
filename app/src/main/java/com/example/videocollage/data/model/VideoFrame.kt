package com.example.videocollage.data.model

import android.graphics.Bitmap

data class VideoFrame(
    val bitmap: Bitmap,
    val timestampMs: Long
)