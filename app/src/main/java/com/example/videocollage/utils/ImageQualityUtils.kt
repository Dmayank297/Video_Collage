package com.example.videocollage.utils

import android.graphics.Bitmap

object ImageQualityUtils {

    fun laplacianVariance(bitmap: Bitmap): Double {
        val gray = toGrayscale(bitmap)
        val width = gray.size
        val height = if (width > 0) gray[0].size else 0
        if (width < 3 || height < 3) return 0.0

        val laplacianValues = mutableListOf<Double>()

        for (x in 1 until width - 1) {
            for (y in 1 until height - 1) {
                val value = (-4 * gray[x][y]) +
                        gray[x - 1][y] + gray[x + 1][y] +
                        gray[x][y - 1] + gray[x][y + 1]
                laplacianValues.add(value.toDouble())
            }
        }

        val mean = laplacianValues.average()
        val variance = laplacianValues.sumOf { (it - mean) * (it - mean) } / laplacianValues.size
        return variance
    }

    private fun toGrayscale(bitmap: Bitmap): Array<IntArray> {
        val width = bitmap.width
        val height = bitmap.height
        val gray = Array(width) { IntArray(height) }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16 and 0xFF)
                val g = (pixel shr 8 and 0xFF)
                val b = (pixel and 0xFF)
                gray[x][y] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
        }
        return gray
    }
}