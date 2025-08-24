package com.example.alkaid.data.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SensorDataTest {

    @Test
    fun `LocationData formatting works`() {
        val loc = LocationData(37.7749, -122.4194, 10.0, 5.0f)
        assertEquals("37.774900°, -122.419400°", loc.getFormattedLatLng())
        assertEquals("10.0 m", loc.getFormattedAltitude())
        assertEquals("±5.0 m", loc.getFormattedAccuracy())
    }

    @Test
    fun `LocationData formatting handles nulls`() {
        val loc = LocationData(0.0, 0.0, null, null)
        assertEquals("0.000000°, 0.000000°", loc.getFormattedLatLng())
        assertEquals("N/A", loc.getFormattedAltitude())
        assertEquals("N/A", loc.getFormattedAccuracy())
    }

    @Test
    fun `GyroscopeData magnitude and formatting`() {
        val gyro = GyroscopeData(1.0f, 2.0f, 2.0f) // magnitude = 3
        assertEquals(3.0f, gyro.getMagnitude())
        assertEquals("3.000 rad/s", gyro.getFormattedMagnitude())
    }

    @Test
    fun `AccelerometerData magnitude and formatting`() {
        val acc = AccelerometerData(0.0f, 3.0f, 4.0f) // magnitude = 5
        assertEquals(5.0f, acc.getMagnitude())
        assertEquals("5.000 m/s²", acc.getFormattedMagnitude())
    }

    @Test
    fun `MagnetometerData magnitude and formatting`() {
        val mag = MagnetometerData(6.0f, 8.0f, 0.0f) // magnitude = 10
        assertEquals(10.0f, mag.getMagnitude())
        assertEquals("10.00 µT", mag.getFormattedMagnitude())
    }

    @Test
    fun `LightData formatting`() {
        val light = LightData(123.456f)
        assertEquals("123.5 lx", light.getFormattedIlluminance())
    }

    @Test
    fun `HumidityData formatting`() {
        val humidity = HumidityData(55.55f)
        assertEquals("55.6%", humidity.getFormattedHumidity())
    }

    @Test
    fun `SensorType fromSettingsKey returns correct enum or null`() {
        assertEquals(SensorType.BAROMETER, SensorType.fromSettingsKey("show_barometer"))
        assertEquals(SensorType.GYROSCOPE, SensorType.fromSettingsKey("show_gyroscope"))
        assertNull(SensorType.fromSettingsKey("not_a_key"))
    }
}

