package com.example.alkaid.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.alkaid.data.sensor.MagnetometerData
import com.example.alkaid.data.sensor.SensorResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Repository for magnetometer sensor data.
 * Provides magnetic field data in µT (microtesla) for X, Y, and Z axes.
 */
class MagnetometerRepository(private val context: Context) : BaseSensorRepository<MagnetometerData> {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val magnetometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    override fun getSensorData(): Flow<SensorResult> = callbackFlow {
        if (magnetometerSensor == null) {
            trySend(SensorResult.Error("Magnetometer sensor not available"))
            close()
            return@callbackFlow
        }

        trySend(SensorResult.Loading)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        val magnetometerData = MagnetometerData(
                            x = it.values[0],
                            y = it.values[1],
                            z = it.values[2]
                        )
                        trySend(SensorResult.Data(magnetometerData))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }

        val registered = sensorManager.registerListener(
            sensorEventListener,
            magnetometerSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (!registered) {
            trySend(SensorResult.Error("Failed to register magnetometer sensor"))
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
