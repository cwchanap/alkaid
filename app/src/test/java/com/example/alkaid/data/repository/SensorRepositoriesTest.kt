package com.example.alkaid.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.alkaid.data.sensor.SensorResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorRepositoriesTest {

    @Test
    fun `repositories emit sensor unavailable errors when sensor is missing`() = runBlocking {
        assertUnavailable(
            repository = AccelerometerRepository(createContext(sensorType = Sensor.TYPE_ACCELEROMETER)),
            expectedMessage = "Accelerometer sensor not available"
        )
        assertUnavailable(
            repository = BarometerRepository(createContext(sensorType = Sensor.TYPE_PRESSURE)),
            expectedMessage = "Barometer sensor not available on this device"
        )
        assertUnavailable(
            repository = GyroscopeRepository(createContext(sensorType = Sensor.TYPE_GYROSCOPE)),
            expectedMessage = "Gyroscope sensor not available on this device"
        )
        assertUnavailable(
            repository = TemperatureRepository(createContext(sensorType = Sensor.TYPE_AMBIENT_TEMPERATURE)),
            expectedMessage = "Temperature sensor not available on this device"
        )
        assertUnavailable(
            repository = MagnetometerRepository(createContext(sensorType = Sensor.TYPE_MAGNETIC_FIELD)),
            expectedMessage = "Magnetometer sensor not available"
        )
        assertUnavailable(
            repository = LightSensorRepository(createContext(sensorType = Sensor.TYPE_LIGHT)),
            expectedMessage = "Light sensor not available"
        )
        assertUnavailable(
            repository = HumiditySensorRepository(createContext(sensorType = Sensor.TYPE_RELATIVE_HUMIDITY)),
            expectedMessage = "Humidity sensor not available"
        )
    }

    @Test
    fun `repositories emit registration failure errors when listener registration fails`() = runBlocking {
        assertRegistrationFailure(
            repository = AccelerometerRepository(
                createContext(
                    sensorType = Sensor.TYPE_ACCELEROMETER,
                    sensor = mockk(),
                    registerListenerResult = false
                )
            ),
            expectedMessage = "Failed to register accelerometer sensor"
        )
        assertRegistrationFailure(
            repository = BarometerRepository(
                createContext(
                    sensorType = Sensor.TYPE_PRESSURE,
                    sensor = mockk(),
                    registerListenerResult = false
                )
            ),
            expectedMessage = "Failed to register barometer sensor listener"
        )
        assertRegistrationFailure(
            repository = GyroscopeRepository(
                createContext(
                    sensorType = Sensor.TYPE_GYROSCOPE,
                    sensor = mockk(),
                    registerListenerResult = false
                )
            ),
            expectedMessage = "Failed to register gyroscope sensor listener"
        )
        assertRegistrationFailure(
            repository = TemperatureRepository(
                createContext(
                    sensorType = Sensor.TYPE_AMBIENT_TEMPERATURE,
                    sensor = mockk(),
                    registerListenerResult = false
                )
            ),
            expectedMessage = "Failed to register temperature sensor listener"
        )
        assertRegistrationFailure(
            repository = MagnetometerRepository(
                createContext(
                    sensorType = Sensor.TYPE_MAGNETIC_FIELD,
                    sensor = mockk(),
                    registerListenerResult = false
                )
            ),
            expectedMessage = "Failed to register magnetometer sensor"
        )
        assertRegistrationFailure(
            repository = LightSensorRepository(
                createContext(
                    sensorType = Sensor.TYPE_LIGHT,
                    sensor = mockk(),
                    registerListenerResult = false
                )
            ),
            expectedMessage = "Failed to register light sensor"
        )
        assertRegistrationFailure(
            repository = HumiditySensorRepository(
                createContext(
                    sensorType = Sensor.TYPE_RELATIVE_HUMIDITY,
                    sensor = mockk(),
                    registerListenerResult = false
                )
            ),
            expectedMessage = "Failed to register humidity sensor"
        )
    }

    private suspend fun assertUnavailable(
        repository: BaseSensorRepository<*>,
        expectedMessage: String
    ) {
        val result = repository.getSensorData().first()

        assertTrue(result is SensorResult.Error)
        assertEquals(expectedMessage, (result as SensorResult.Error).message)
    }

    private suspend fun assertRegistrationFailure(
        repository: BaseSensorRepository<*>,
        expectedMessage: String
    ) {
        val results = repository.getSensorData().take(2).toList()

        assertEquals(SensorResult.Loading, results[0])
        assertTrue(results[1] is SensorResult.Error)
        assertEquals(expectedMessage, (results[1] as SensorResult.Error).message)
    }

    private fun createContext(
        sensorType: Int,
        sensor: Sensor? = null,
        registerListenerResult: Boolean = true
    ): Context {
        val context = mockk<Context>()
        val sensorManager = mockk<SensorManager>()

        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
        every { sensorManager.getDefaultSensor(sensorType) } returns sensor
        every { sensorManager.unregisterListener(any<SensorEventListener>()) } returns Unit

        if (sensor != null) {
            every {
                sensorManager.registerListener(
                    any<SensorEventListener>(),
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            } returns registerListenerResult
        }

        return context
    }
}
