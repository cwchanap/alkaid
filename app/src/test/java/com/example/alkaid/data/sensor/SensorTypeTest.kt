package com.example.alkaid.data.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class SensorTypeTest {

    @Test
    fun `fromSettingsKey returns correct SensorType for each sensor`() {
        SensorType.values().forEach { sensorType ->
            val result = SensorType.fromSettingsKey(sensorType.settingsKey)
            assertEquals(
                "fromSettingsKey should return $sensorType for key ${sensorType.settingsKey}",
                sensorType,
                result
            )
        }
    }

    @Test
    fun `fromSettingsKey returns null for unknown key`() {
        assertNull(SensorType.fromSettingsKey("unknown_key"))
    }

    @Test
    fun `all sensor types have non-empty display names`() {
        SensorType.values().forEach { sensorType ->
            assertNotNull(sensorType.displayName)
            assert(sensorType.displayName.isNotEmpty()) {
                "$sensorType should have a non-empty display name"
            }
        }
    }

    @Test
    fun `all sensor types have non-empty units`() {
        SensorType.values().forEach { sensorType ->
            assertNotNull(sensorType.unit)
            assert(sensorType.unit.isNotEmpty()) {
                "$sensorType should have a non-empty unit"
            }
        }
    }

    @Test
    fun `all sensor settings keys are unique`() {
        val keys = SensorType.values().map { it.settingsKey }
        assertEquals("All settings keys should be unique", keys.size, keys.toSet().size)
    }

    @Test
    fun `GPS has correct display name`() {
        assertEquals("GPS Location", SensorType.GPS.displayName)
    }

    @Test
    fun `BAROMETER has correct unit`() {
        assertEquals("hPa", SensorType.BAROMETER.unit)
    }

    @Test
    fun `GYROSCOPE has correct unit`() {
        assertEquals("rad/s", SensorType.GYROSCOPE.unit)
    }

    @Test
    fun `ACCELEROMETER has correct unit`() {
        assertEquals("m/s²", SensorType.ACCELEROMETER.unit)
    }

    @Test
    fun `MAGNETOMETER has correct unit`() {
        assertEquals("µT", SensorType.MAGNETOMETER.unit)
    }

    @Test
    fun `LIGHT has correct unit`() {
        assertEquals("lx", SensorType.LIGHT.unit)
    }

    @Test
    fun `HUMIDITY has correct unit`() {
        assertEquals("%", SensorType.HUMIDITY.unit)
    }
}
