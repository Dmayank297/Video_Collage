package com.example.videocollage.domain

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset

data class CollageSlot(
    val personId: Int,
    val bitmap: Bitmap,
    var offset: Offset = Offset.Zero,
    var scale: Float = 1f
)
