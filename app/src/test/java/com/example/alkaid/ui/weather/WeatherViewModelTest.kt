package com.example.alkaid.ui.weather

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.alkaid.data.security.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class WeatherViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var application: Application
    private lateinit var viewModel: WeatherViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        // Clear any API key persisted from a previous test before creating the ViewModel
        // so that initial-state assertions are always deterministic.
        SecureStorage(application).clearAll()
        viewModel = WeatherViewModel(application)
    }

    @After
    fun tearDown() {
        // Remove any API key saved during the test to avoid cross-test pollution.
        SecureStorage(application).clearAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial apiKeyValid is false when no API key stored`() {
        assertFalse(viewModel.apiKeyValid.value)
    }

    @Test
    fun `hasApiKey returns false when no key stored`() {
        assertFalse(viewModel.hasApiKey())
    }

    @Test
    fun `getLocationString returns Unknown location when no location set`() {
        assertEquals("Unknown location", viewModel.getLocationString())
    }

    @Test
    fun `refreshWeather sets NoApiKey state when apiKey not valid`() {
        // apiKeyValid is false after init (no key stored in test env)
        viewModel.refreshWeather()
        assertEquals(WeatherViewState.NoApiKey, viewModel.weatherState.value)
    }

    @Test
    fun `removeApiKey sets NoApiKey state`() {
        viewModel.removeApiKey()
        assertEquals(WeatherViewState.NoApiKey, viewModel.weatherState.value)
    }

    @Test
    fun `removeApiKey sets apiKeyValid to false`() {
        viewModel.removeApiKey()
        assertFalse(viewModel.apiKeyValid.value)
    }

    @Test
    fun `saveApiKey with blank string does not validate key`() {
        viewModel.saveApiKey("   ")
        assertFalse(viewModel.apiKeyValid.value)
    }

    @Test
    fun `saveApiKey with empty string does not validate key`() {
        viewModel.saveApiKey("")
        assertFalse(viewModel.apiKeyValid.value)
    }

    @Test
    fun `saveApiKey with valid key sets apiKeyValid to true`() {
        viewModel.saveApiKey("test_api_key_12345")
        assertTrue("apiKeyValid should be true after saving a non-blank key", viewModel.apiKeyValid.value)
    }

    @Test
    fun `saveApiKey with valid key and no location sets WaitingForLocation state`() {
        viewModel.saveApiKey("test_api_key_12345")
        // No location is available, so state should be WaitingForLocation
        assertEquals(WeatherViewState.WaitingForLocation, viewModel.weatherState.value)
    }
}
