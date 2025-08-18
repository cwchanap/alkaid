package com.example.alkaid.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.alkaid.data.sensor.AccelerometerData
import com.example.alkaid.data.sensor.SensorResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Repository for accelerometer sensor data.
 * Provides acceleration data in m/s² for X, Y, and Z axes.
 */
class AccelerometerRepository(private val context: Context) : BaseSensorRepository<AccelerometerData> {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    override fun getSensorData(): Flow<SensorResult> = callbackFlow {
        if (accelerometerSensor == null) {
            trySend(SensorResult.Error("Accelerometer sensor not available"))
            close()
            return@callbackFlow
        }

        trySend(SensorResult.Loading)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val accelerometerData = AccelerometerData(
                            x = it.values[0],
                            y = it.values[1],
                            z = it.values[2]
                        )
                        trySend(SensorResult.Data(accelerometerData))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }

        val registered = sensorManager.registerListener(
            sensorEventListener,
            accelerometerSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (!registered) {
            trySend(SensorResult.Error("Failed to register accelerometer sensor"))
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
