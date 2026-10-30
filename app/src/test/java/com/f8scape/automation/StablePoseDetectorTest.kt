package com.f8scape.automation

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StablePoseDetectorTest {

    private lateinit var detector: StablePoseDetector

    @Before
    fun setUp() {
        detector = StablePoseDetector(windowSize = 10, varianceThreshold = 0.35f)
    }

    @Test
    fun constantAcceleration_yieldsHighStabilityConfidence() {
        // Stationary resting on a table
        for (i in 0 until 10) {
            detector.onAccelerometerSample(4.5f, 0.1f, 8.5f, i * 50_000_000L)
        }

        val confidence = detector.getStabilityConfidence()
        assertTrue("Confidence should be very high for stationary samples: $confidence", confidence > 0.90f)
    }

    @Test
    fun noisyAcceleration_yieldsLowStabilityConfidence() {
        // Shaking / moving
        val values = listOf(
            floatArrayOf(1f, 2f, 9f),
            floatArrayOf(-3f, 8f, 2f),
            floatArrayOf(5f, -1f, 12f),
            floatArrayOf(-2f, 6f, 4f),
            floatArrayOf(4f, -3f, 10f),
            floatArrayOf(-1f, 5f, 1f)
        )

        values.forEachIndexed { i, arr ->
            detector.onAccelerometerSample(arr[0], arr[1], arr[2], i * 50_000_000L)
        }

        val confidence = detector.getStabilityConfidence()
        assertTrue("Confidence should be low for noisy samples: $confidence", confidence < 0.50f)
    }

    @Test
    fun tentOrientationCandidate_detectsTiltedLandscapePose() {
        // Landscape tent pose: gravity split between X and Z (Y near zero, resting on edge)
        detector.onGravitySample(6.5f, 0.2f, 7.2f)
        assertTrue(detector.isTentOrientationCandidate())

        // Portrait upright pose (Y is dominant ~9.8): MUST be rejected
        detector.onGravitySample(0.0f, 9.8f, 0.0f)
        assertFalse(detector.isTentOrientationCandidate())

        // Flat on back (Z = 9.8, Y = 0, X = 0): MUST be rejected
        detector.onGravitySample(0.0f, 0.0f, 9.8f)
        assertFalse(detector.isTentOrientationCandidate())
    }
}
