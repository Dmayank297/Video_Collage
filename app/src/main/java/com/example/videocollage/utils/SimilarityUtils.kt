package com.example.videocollage.utils

import kotlin.math.sqrt

object SimilarityUtils {

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Embeddings must be the same size" }

        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0f) 0f else dot / denominator
    }

    fun cosineDistance(a: FloatArray, b: FloatArray): Float {
        return 1f - cosineSimilarity(a, b)
    }
}