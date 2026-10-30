package com.f8scape.fold

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.f8scape.automation.StablePoseDetector
import com.f8scape.util.F8Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

/**
 * Sensor-based posture and stability observer.
 * Listens to hinge angle sensor (if present) and accelerometer/gravity sensors
 * to distinguish resting tent posture from hand movement.
 */
class SensorPostureSource(
    private val context: Context,
    private val poseDetector: StablePoseDetector = StablePoseDetector()
) : PostureSource {

    override val name: String = "Hardware Sensor Posture Source"

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val hingeSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
    private val accelSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)

    override val isAvailable: Boolean = sensorManager != null && (hingeSensor != null || accelSensor != null)

    override fun observePosture(): Flow<PostureData> = callbackFlow {
        if (sensorManager == null) {
            close()
            return@callbackFlow
        }

        var lastHingeAngle: Float? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                when (event.sensor.type) {
                    Sensor.TYPE_HINGE_ANGLE -> {
                        val angle = event.values.firstOrNull() ?: return
                        lastHingeAngle = angle
                        F8Log.d("SensorPosture", "Hinge angle update: $angle°")
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        poseDetector.onAccelerometerSample(x, y, z, event.timestamp)
                    }
                    Sensor.TYPE_GRAVITY -> {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        poseDetector.onGravitySample(x, y, z)
                    }
                }

                val stability = poseDetector.getStabilityConfidence()
                val isTentPose = poseDetector.isTentOrientationCandidate()

                val posture = when {
                    lastHingeAngle != null -> {
                        val angle = lastHingeAngle!!
                        when {
                            angle < 20f -> FoldPosture.CLOSED
                            angle in 45f..135f && isTentPose -> FoldPosture.TENT_CANDIDATE
                            angle in 30f..160f -> FoldPosture.HALF_OPENED
                            angle >= 160f -> FoldPosture.FLAT
                            else -> FoldPosture.UNKNOWN
                        }
                    }
                    isTentPose && stability > 0.65f -> FoldPosture.TENT_CANDIDATE
                    else -> FoldPosture.UNKNOWN
                }

                trySend(
                    PostureData(
                        posture = posture,
                        isCoverScreenActive = true,
                        stabilityConfidence = stability,
                        hingeAngleDeg = lastHingeAngle,
                        source = name,
                        timestampMs = System.currentTimeMillis()
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No-op
            }
        }

        hingeSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        gravitySensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        awaitClose {
            sensorManager.unregisterListener(listener)
            F8Log.d("SensorPosture", "Unregistered sensor listeners")
        }
    }
}
