package com.example.alkaid.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class SecureStorageTest {

    private lateinit var context: Context
    private lateinit var secureStorage: SecureStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        secureStorage = SecureStorage(context)
        secureStorage.clearAll()
    }

    @After
    fun tearDown() {
        secureStorage.clearAll()
    }

    @Test
    fun `saveWeatherApiKey stores value that getWeatherApiKey returns`() {
        secureStorage.saveWeatherApiKey("weather-key")

        assertEquals("weather-key", secureStorage.getWeatherApiKey())
    }

    @Test
    fun `hasWeatherApiKey returns false when key missing`() {
        assertFalse(secureStorage.hasWeatherApiKey())
    }

    @Test
    fun `hasWeatherApiKey returns false when key is blank`() {
        secureStorage.saveWeatherApiKey("   ")

        assertFalse(secureStorage.hasWeatherApiKey())
    }

    @Test
    fun `hasWeatherApiKey returns true when key is present`() {
        secureStorage.saveWeatherApiKey("weather-key")

        assertTrue(secureStorage.hasWeatherApiKey())
    }

    @Test
    fun `removeWeatherApiKey clears stored value`() {
        secureStorage.saveWeatherApiKey("weather-key")

        secureStorage.removeWeatherApiKey()

        assertNull(secureStorage.getWeatherApiKey())
        assertFalse(secureStorage.hasWeatherApiKey())
    }

    @Test
    fun `clearAll removes stored key`() {
        secureStorage.saveWeatherApiKey("weather-key")

        secureStorage.clearAll()

        assertNull(secureStorage.getWeatherApiKey())
        assertFalse(secureStorage.hasWeatherApiKey())
    }
}
