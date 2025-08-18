package com.example.alkaid.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.alkaid.data.sensor.LightData
import com.example.alkaid.data.sensor.SensorResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Repository for light sensor data.
 * Provides illuminance data in lux (lx).
 */
class LightSensorRepository(private val context: Context) : BaseSensorRepository<LightData> {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    override fun getSensorData(): Flow<SensorResult> = callbackFlow {
        if (lightSensor == null) {
            trySend(SensorResult.Error("Light sensor not available"))
            close()
            return@callbackFlow
        }

        trySend(SensorResult.Loading)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_LIGHT) {
                        val lightData = LightData(
                            illuminance = it.values[0]
                        )
                        trySend(SensorResult.Data(lightData))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }

        val registered = sensorManager.registerListener(
            sensorEventListener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (!registered) {
            trySend(SensorResult.Error("Failed to register light sensor"))
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
