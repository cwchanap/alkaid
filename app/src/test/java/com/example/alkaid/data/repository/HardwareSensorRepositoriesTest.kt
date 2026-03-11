package com.example.alkaid.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.alkaid.data.sensor.SensorResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for hardware sensor repositories when the hardware sensor is unavailable
 * (the common case in test environments / emulators).
 * Robolectric does not expose real hardware sensors, so getDefaultSensor() returns null
 * for most sensor types – which is the "sensor not available" path we want to exercise.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class HardwareSensorRepositoriesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ---- BarometerRepository ----

    @Test
    fun `BarometerRepository emits error when sensor unavailable`() = runBlocking {
        val repo = BarometerRepository(context)
        val result = repo.getSensorData().first()
        assertTrue(result is SensorResult.Error)
        assertEquals(
            "Barometer sensor not available on this device",
            (result as SensorResult.Error).message
        )
    }

    // ---- TemperatureRepository ----

    @Test
    fun `TemperatureRepository emits error when sensor unavailable`() = runBlocking {
        val repo = TemperatureRepository(context)
        val result = repo.getSensorData().first()
        assertTrue(result is SensorResult.Error)
        assertEquals(
            "Temperature sensor not available on this device",
            (result as SensorResult.Error).message
        )
    }

    // ---- GyroscopeRepository ----

    @Test
    fun `GyroscopeRepository emits error when sensor unavailable`() = runBlocking {
        val repo = GyroscopeRepository(context)
        val result = repo.getSensorData().first()
        assertTrue(result is SensorResult.Error)
        assertEquals(
            "Gyroscope sensor not available on this device",
            (result as SensorResult.Error).message
        )
    }

    // ---- AccelerometerRepository ----

    @Test
    fun `AccelerometerRepository emits error when sensor unavailable`() = runBlocking {
        val repo = AccelerometerRepository(context)
        val result = repo.getSensorData().first()
        assertTrue(result is SensorResult.Error)
        assertEquals(
            "Accelerometer sensor not available",
            (result as SensorResult.Error).message
        )
    }

    // ---- MagnetometerRepository ----

    @Test
    fun `MagnetometerRepository emits error when sensor unavailable`() = runBlocking {
        val repo = MagnetometerRepository(context)
        val result = repo.getSensorData().first()
        assertTrue(result is SensorResult.Error)
        assertEquals(
            "Magnetometer sensor not available",
            (result as SensorResult.Error).message
        )
    }

    // ---- LightSensorRepository ----

    @Test
    fun `LightSensorRepository emits error when sensor unavailable`() = runBlocking {
        val repo = LightSensorRepository(context)
        val result = repo.getSensorData().first()
        assertTrue(result is SensorResult.Error)
        assertEquals(
            "Light sensor not available",
            (result as SensorResult.Error).message
        )
    }

    // ---- HumiditySensorRepository ----

    @Test
    fun `HumiditySensorRepository emits error when sensor unavailable`() = runBlocking {
        val repo = HumiditySensorRepository(context)
        val result = repo.getSensorData().first()
        assertTrue(result is SensorResult.Error)
        assertEquals(
            "Humidity sensor not available",
            (result as SensorResult.Error).message
        )
    }
}
