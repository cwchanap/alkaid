package com.example.alkaid.data.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorDataTest {

    @Test
    fun `test LocationData formatting`() {
        val locationData = LocationData(37.7749, -122.4194, 100.0, 10.0f)
        assertEquals("37.774900°, -122.419400°", locationData.getFormattedLatLng())
        assertEquals("100.0 m", locationData.getFormattedAltitude())
                assertEquals("±10.0 m", locationData.getFormattedAccuracy())
    }

    @Test
    fun `test GyroscopeData formatting`() {
        val gyroscopeData = GyroscopeData(1.0f, 2.0f, 3.0f)
        assertEquals("3.742 rad/s", gyroscopeData.getFormattedMagnitude())
    }

    @Test
    fun `test AccelerometerData formatting`() {
        val accelerometerData = AccelerometerData(1.0f, 2.0f, 3.0f)
        assertEquals("3.742 m/s²", accelerometerData.getFormattedMagnitude())
    }

    @Test
    fun `test MagnetometerData formatting`() {
        val magnetometerData = MagnetometerData(1.0f, 2.0f, 3.0f)
        assertEquals("3.74 µT", magnetometerData.getFormattedMagnitude())
    }

    @Test
    fun `test LightData formatting`() {
        val lightData = LightData(100.0f)
        assertEquals("100.0 lx", lightData.getFormattedIlluminance())
    }

    @Test
    fun `test HumidityData formatting`() {
        val humidityData = HumidityData(50.0f)
        assertEquals("50.0%", humidityData.getFormattedHumidity())
    }
}