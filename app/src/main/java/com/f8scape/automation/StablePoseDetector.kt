package com.f8scape.automation

import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects whether the device is in a stable tent posture resting on a surface.
 * Computes acceleration variance over a sliding window to reject hand movement and vibration.
 */
class StablePoseDetector(
    private val windowSize: Int = 20,
    private val varianceThreshold: Float = 0.35f
) {
    private val sampleWindow = ArrayDeque<FloatArray>(windowSize)
    private var lastGravityX = 0f
    private var lastGravityY = 0f
    private var lastGravityZ = 0f

    @Synchronized
    fun onAccelerometerSample(x: Float, y: Float, z: Float, timestampNs: Long) {
        if (sampleWindow.size >= windowSize) {
            sampleWindow.removeFirst()
        }
        sampleWindow.addLast(floatArrayOf(x, y, z))
    }

    @Synchronized
    fun onGravitySample(x: Float, y: Float, z: Float) {
        lastGravityX = x
        lastGravityY = y
        lastGravityZ = z
    }

    /**
     * Returns 0.0 (high movement) to 1.0 (completely stationary).
     */
    @Synchronized
    fun getStabilityConfidence(): Float {
        if (sampleWindow.size < 5) return 0f

        var sumX = 0f
        var sumY = 0f
        var sumZ = 0f

        for (sample in sampleWindow) {
            sumX += sample[0]
            sumY += sample[1]
            sumZ += sample[2]
        }

        val count = sampleWindow.size
        val meanX = sumX / count
        val meanY = sumY / count
        val meanZ = sumZ / count

        var varSum = 0f
        for (sample in sampleWindow) {
            val dx = sample[0] - meanX
            val dy = sample[1] - meanY
            val dz = sample[2] - meanZ
            varSum += (dx * dx + dy * dy + dz * dz)
        }

        val variance = varSum / count
        val confidence = 1.0f - (variance / varianceThreshold).coerceIn(0f, 1f)
        return confidence
    }

    /**
     * Checks if gravity vector indicates the phone is resting upright or propped at an angle,
     * typical of a tent posture (neither fully flat on back Z ~ 9.8, nor horizontal flat screen).
     */
    @Synchronized
    fun isTentOrientationCandidate(): Boolean {
        val totalG = sqrt(lastGravityX * lastGravityX + lastGravityY * lastGravityY + lastGravityZ * lastGravityZ)
        if (totalG < 1.0f) return false

        val normalizedZ = abs(lastGravityZ / totalG)
        val normalizedY = abs(lastGravityY / totalG)
        val normalizedX = abs(lastGravityX / totalG)

        // In tent posture resting on edges, the device is tilted between 45 and 85 degrees
        // meaning neither Z is dominant (not lying flat) nor purely vertical
        val isNotLyingFlat = normalizedZ < 0.85f
        val isTilted = (normalizedY > 0.25f || normalizedX > 0.25f)
        return isNotLyingFlat && isTilted
    }

    @Synchronized
    fun reset() {
        sampleWindow.clear()
        lastGravityX = 0f
        lastGravityY = 0f
        lastGravityZ = 0f
    }
}
