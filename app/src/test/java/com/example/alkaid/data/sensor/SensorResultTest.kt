package com.example.alkaid.data.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorResultTest {

    @Test
    fun `Loading is a valid SensorResult`() {
        val result: SensorResult = SensorResult.Loading
        assertTrue(result is SensorResult.Loading)
    }

    @Test
    fun `Data wraps value correctly`() {
        val result: SensorResult = SensorResult.Data(42.5f)
        assertTrue(result is SensorResult.Data<*>)
        assertEquals(42.5f, (result as SensorResult.Data<*>).value)
    }

    @Test
    fun `Data works with LocationData`() {
        val locationData = LocationData(37.7749, -122.4194)
        val result: SensorResult = SensorResult.Data(locationData)
        assertTrue(result is SensorResult.Data<*>)
        val value = (result as SensorResult.Data<*>).value as LocationData
        assertEquals(37.7749, value.latitude, 0.0)
        assertEquals(-122.4194, value.longitude, 0.0)
    }

    @Test
    fun `Error stores message correctly`() {
        val result: SensorResult = SensorResult.Error("Sensor not available")
        assertTrue(result is SensorResult.Error)
        assertEquals("Sensor not available", (result as SensorResult.Error).message)
    }

    @Test
    fun `Data equality works correctly`() {
        val result1 = SensorResult.Data(10.0f)
        val result2 = SensorResult.Data(10.0f)
        val result3 = SensorResult.Data(20.0f)
        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }

    @Test
    fun `Error equality works correctly`() {
        val error1 = SensorResult.Error("msg")
        val error2 = SensorResult.Error("msg")
        val error3 = SensorResult.Error("different")
        assertEquals(error1, error2)
        assertNotEquals(error1, error3)
    }

    @Test
    fun `Loading equals itself`() {
        assertEquals(SensorResult.Loading, SensorResult.Loading)
    }
}
