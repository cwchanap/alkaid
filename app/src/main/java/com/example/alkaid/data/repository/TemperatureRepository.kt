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
 * Repository for temperature sensor data.
 * Provides ambient temperature readings in degrees Celsius.
 */
class TemperatureRepository(private val context: Context) : BaseSensorRepository<Float> {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

    override fun getSensorData(): Flow<SensorResult> = callbackFlow {
        if (temperatureSensor == null) {
            trySend(SensorResult.Error("Temperature sensor not available on this device"))
            close()
            return@callbackFlow
        }

        trySend(SensorResult.Loading)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                    val temperature = event.values[0] // Temperature in degrees Celsius
                    trySend(SensorResult.Data(temperature))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }

        val isRegistered = sensorManager.registerListener(
            sensorEventListener,
            temperatureSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (!isRegistered) {
            trySend(SensorResult.Error("Failed to register temperature sensor listener"))
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
