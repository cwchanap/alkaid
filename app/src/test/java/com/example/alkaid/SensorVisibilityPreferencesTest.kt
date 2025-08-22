package com.example.alkaid

import android.content.Context
import android.content.SharedPreferences
import com.example.alkaid.data.preferences.SensorVisibilityPreferences
import com.example.alkaid.data.sensor.SensorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SensorVisibilityPreferences.
 * Tests the basic functionality of getting and setting sensor visibility preferences.
 */
class SensorVisibilityPreferencesTest {

    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var sensorVisibilityPreferences: SensorVisibilityPreferences

    @Before
    fun setup() {
        mockContext = mockk()
        mockSharedPreferences = mockk()
        mockEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        // Fallback for keys not explicitly stubbed below: return default value
        // For sensors not explicitly stubbed in tests, default to hidden (false)
        every { mockSharedPreferences.getBoolean(any(), any()) } answers { false }
        // Ensure chained editor calls return the same editor instance when used
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit

        sensorVisibilityPreferences = SensorVisibilityPreferences(mockContext)
    }

    @Test
    fun `isSensorVisible returns default value when preference not set`() {
        // Given
        every { mockSharedPreferences.getBoolean(SensorType.BAROMETER.settingsKey, true) } returns true

        // When
        val result = sensorVisibilityPreferences.isSensorVisible(SensorType.BAROMETER)

        // Then
        assertTrue(result)
        verify { mockSharedPreferences.getBoolean(SensorType.BAROMETER.settingsKey, true) }
    }

    @Test
    fun `setSensorVisible updates preference`() {
        // Given
        val isVisible = false

        // When
        sensorVisibilityPreferences.setSensorVisible(SensorType.GPS, isVisible)

        // Then
        verify { mockEditor.putBoolean(SensorType.GPS.settingsKey, isVisible) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `getAllSensorVisibilityStates returns map of all sensors`() {
        // Given
        every { mockSharedPreferences.getBoolean(SensorType.BAROMETER.settingsKey, true) } returns true
        every { mockSharedPreferences.getBoolean(SensorType.GYROSCOPE.settingsKey, true) } returns false
        every { mockSharedPreferences.getBoolean(SensorType.TEMPERATURE.settingsKey, true) } returns true
        every { mockSharedPreferences.getBoolean(SensorType.GPS.settingsKey, true) } returns false

        // When
        val result = sensorVisibilityPreferences.getAllSensorVisibilityStates()

        // Then
        assertEquals(SensorType.values().size, result.size)
        assertTrue(result[SensorType.BAROMETER] ?: false)
        assertFalse(result[SensorType.GYROSCOPE] ?: true)
        assertTrue(result[SensorType.TEMPERATURE] ?: false)
        assertFalse(result[SensorType.GPS] ?: true)
    }

    @Test
    fun `getVisibleSensorsFlow returns only visible sensors`() = runTest {
        // Given
        every { mockSharedPreferences.getBoolean(SensorType.BAROMETER.settingsKey, true) } returns true
        every { mockSharedPreferences.getBoolean(SensorType.GYROSCOPE.settingsKey, true) } returns false
        every { mockSharedPreferences.getBoolean(SensorType.TEMPERATURE.settingsKey, true) } returns true
        every { mockSharedPreferences.getBoolean(SensorType.GPS.settingsKey, true) } returns false

        // When
        val result = sensorVisibilityPreferences.getVisibleSensorsFlow().first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains(SensorType.BAROMETER))
        assertTrue(result.contains(SensorType.TEMPERATURE))
        assertFalse(result.contains(SensorType.GYROSCOPE))
        assertFalse(result.contains(SensorType.GPS))
    }
}
