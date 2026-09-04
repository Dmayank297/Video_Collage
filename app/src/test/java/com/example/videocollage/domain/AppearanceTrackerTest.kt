package com.example.videocollage.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceTrackerTest {

    @Test
    fun `continuous detections form one appearance`() {
        val tracker = AppearanceTracker(gapToleranceMs = 1000L)
        val detections = listOf(
            TimedDetection(0, 0L),
            TimedDetection(0, 500L),
            TimedDetection(0, 1000L),
            TimedDetection(0, 1500L)
        )

        val appearances = tracker.track(detections)

        assertEquals(1, appearances.size)
        assertEquals(0L, appearances[0].startMs)
        assertEquals(1500L, appearances[0].endMs)
    }

    @Test
    fun `gap larger than tolerance splits into two appearances`() {
        val tracker = AppearanceTracker(gapToleranceMs = 1000L)
        val detections = listOf(
            TimedDetection(0, 0L),
            TimedDetection(0, 500L),
            TimedDetection(0, 3500L),
            TimedDetection(0, 4000L)
        )

        val appearances = tracker.track(detections)

        assertEquals(2, appearances.size)
        assertEquals(0L, appearances[0].startMs)
        assertEquals(500L, appearances[0].endMs)
        assertEquals(3500L, appearances[1].startMs)
        assertEquals(4000L, appearances[1].endMs)
    }

    @Test
    fun `gap exactly at tolerance does not split`() {
        val tracker = AppearanceTracker(gapToleranceMs = 1000L)
        val detections = listOf(
            TimedDetection(0, 0L),
            TimedDetection(0, 1000L)
        )

        val appearances = tracker.track(detections)

        assertEquals(1, appearances.size)
    }

    @Test
    fun `different clusters are tracked independently`() {
        val tracker = AppearanceTracker(gapToleranceMs = 1000L)
        val detections = listOf(
            TimedDetection(0, 0L),
            TimedDetection(1, 0L),
            TimedDetection(0, 500L),
            TimedDetection(1, 500L)
        )

        val appearances = tracker.track(detections)

        assertEquals(2, appearances.size)
        assertEquals(1, appearances.count { it.clusterId == 0 })
        assertEquals(1, appearances.count { it.clusterId == 1 })
    }

    @Test
    fun `single detection creates single-point appearance`() {
        val tracker = AppearanceTracker(gapToleranceMs = 1000L)
        val detections = listOf(TimedDetection(0, 100L))

        val appearances = tracker.track(detections)

        assertEquals(1, appearances.size)
        assertEquals(100L, appearances[0].startMs)
        assertEquals(100L, appearances[0].endMs)
    }

    @Test
    fun `empty input returns empty output`() {
        val tracker = AppearanceTracker()
        val appearances = tracker.track(emptyList())
        assertEquals(0, appearances.size)
    }
}