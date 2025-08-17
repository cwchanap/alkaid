package com.example.alkaid.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.alkaid.data.sensor.GyroscopeData
import com.example.alkaid.data.sensor.SensorResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Repository for gyroscope sensor data.
 * Provides rotation rates around the X, Y, and Z axes in rad/s.
 */
class GyroscopeRepository(private val context: Context) : BaseSensorRepository<GyroscopeData> {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    override fun getSensorData(): Flow<SensorResult> = callbackFlow {
        if (gyroscopeSensor == null) {
            trySend(SensorResult.Error("Gyroscope sensor not available on this device"))
            close()
            return@callbackFlow
        }

        trySend(SensorResult.Loading)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                    val gyroData = GyroscopeData(
                        x = event.values[0], // Rotation rate around X-axis (rad/s)
                        y = event.values[1], // Rotation rate around Y-axis (rad/s)
                        z = event.values[2]  // Rotation rate around Z-axis (rad/s)
                    )
                    trySend(SensorResult.Data(gyroData))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }

        val isRegistered = sensorManager.registerListener(
            sensorEventListener,
            gyroscopeSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (!isRegistered) {
            trySend(SensorResult.Error("Failed to register gyroscope sensor listener"))
        }

        awaitClose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }.distinctUntilChanged()

    override suspend fun startListening() {
        // Implementation handled by the Flow
    }

    override suspend fun stopListening() {
        // Implementation handled by the Flow's awaitClose
    }
}
