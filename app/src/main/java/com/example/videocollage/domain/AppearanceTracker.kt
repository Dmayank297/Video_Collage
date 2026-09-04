package com.example.videocollage.domain

import android.util.Log
import com.example.videocollage.data.model.FaceAppearance

private const val TAG = "AppearanceTracker"

data class TimedDetection(
    val clusterId: Int,
    val timestampMs: Long
)

class AppearanceTracker(
    private val gapToleranceMs: Long = 1000L
) {

    fun track(detections: List<TimedDetection>): List<FaceAppearance> {
        val appearances = mutableListOf<FaceAppearance>()

        val grouped = detections.groupBy { it.clusterId }

        for ((clusterId, clusterDetections) in grouped) {
            val sorted = clusterDetections.sortedBy { it.timestampMs }

            var appearanceStart = sorted.first().timestampMs
            var lastSeen = sorted.first().timestampMs

            for (i in 1 until sorted.size) {
                val current = sorted[i].timestampMs
                val gap = current - lastSeen

                Log.d(TAG, "cluster=$clusterId gap=${gap}ms (tolerance=${gapToleranceMs}ms) " +
                        if (gap > gapToleranceMs) "-> SPLIT" else "-> same appearance")

                if (gap > gapToleranceMs) {
                    appearances.add(FaceAppearance(clusterId, appearanceStart, lastSeen))
                    appearanceStart = current
                }

                lastSeen = current
            }

            appearances.add(FaceAppearance(clusterId, appearanceStart, lastSeen))
        }

        return appearances
    }
}