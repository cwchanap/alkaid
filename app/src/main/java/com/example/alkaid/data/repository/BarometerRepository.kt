package com.example.alkaid.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.alkaid.data.sensor.SensorResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Repository for barometric pressure sensor data.
 * Provides atmospheric pressure readings in hPa (hectopascals).
 */
class BarometerRepository(private val context: Context) : BaseSensorRepository<Float> {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    override fun getSensorData(): Flow<SensorResult> = callbackFlow {
        if (pressureSensor == null) {
            trySend(SensorResult.Error("Barometer sensor not available on this device"))
            close()
            return@callbackFlow
        }

        trySend(SensorResult.Loading)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_PRESSURE) {
                    val pressure = event.values[0] // Pressure in hPa
                    trySend(SensorResult.Data(pressure))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }

        val isRegistered = sensorManager.registerListener(
            sensorEventListener,
            pressureSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (!isRegistered) {
            trySend(SensorResult.Error("Failed to register barometer sensor listener"))
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
