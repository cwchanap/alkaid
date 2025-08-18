package com.example.alkaid.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.alkaid.data.sensor.HumidityData
import com.example.alkaid.data.sensor.SensorResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Repository for humidity sensor data.
 * Provides relative humidity data as percentage.
 */
class HumiditySensorRepository(private val context: Context) : BaseSensorRepository<HumidityData> {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val humiditySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)

    override fun getSensorData(): Flow<SensorResult> = callbackFlow {
        if (humiditySensor == null) {
            trySend(SensorResult.Error("Humidity sensor not available"))
            close()
            return@callbackFlow
        }

        trySend(SensorResult.Loading)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_RELATIVE_HUMIDITY) {
                        val humidityData = HumidityData(
                            humidity = it.values[0]
                        )
                        trySend(SensorResult.Data(humidityData))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }

        val registered = sensorManager.registerListener(
            sensorEventListener,
            humiditySensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (!registered) {
            trySend(SensorResult.Error("Failed to register humidity sensor"))
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
