package com.example.alkaid.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.alkaid.data.security.SecureStorage
import com.example.alkaid.data.weather.WeatherApiService
import com.example.alkaid.data.weather.Clouds
import com.example.alkaid.data.weather.MainWeatherData
import com.example.alkaid.data.weather.Sys
import com.example.alkaid.data.weather.Weather
import com.example.alkaid.data.weather.WeatherResponse
import com.example.alkaid.data.weather.Wind
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class WeatherRepositoryTest {

    private lateinit var context: Context
    private lateinit var secureStorage: SecureStorage
    private lateinit var weatherApiService: WeatherApiService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        secureStorage = mockk()
        weatherApiService = mockk()
    }

    @Test
    fun `getWeatherByCoordinates emits no api key when stored key is blank`() = runTest {
        every { secureStorage.getWeatherApiKey() } returns " "

        val repository = WeatherRepository(
            context = context,
            secureStorage = secureStorage,
            weatherApiService = weatherApiService,
            networkAvailable = { true }
        )

        val results = repository.getWeatherByCoordinates(35.0, 139.0).toList()

        assertEquals(listOf(WeatherResult.Loading, WeatherResult.NoApiKey), results)
    }

    @Test
    fun `getWeatherByCoordinates emits success when api returns weather`() = runTest {
        every { secureStorage.getWeatherApiKey() } returns "api-key"
        coEvery {
            weatherApiService.getCurrentWeather(35.0, 139.0, "api-key", "metric")
        } returns sampleWeatherResponse(cityName = "Tokyo")

        val repository = WeatherRepository(
            context = context,
            secureStorage = secureStorage,
            weatherApiService = weatherApiService,
            networkAvailable = { true }
        )

        val results = repository.getWeatherByCoordinates(35.0, 139.0).toList()

        assertEquals(2, results.size)
        assertEquals(WeatherResult.Loading, results.first())
        val success = results[1] as WeatherResult.Success
        assertEquals("35.0000°, 139.0000°", success.data.location)
        assertEquals("22°C", success.data.temperature)
        assertEquals("Clear sky", success.data.condition)
        coVerify { weatherApiService.getCurrentWeather(35.0, 139.0, "api-key", "metric") }
    }

    @Test
    fun `getWeatherByCity emits success when api returns weather`() = runTest {
        every { secureStorage.getWeatherApiKey() } returns "api-key"
        coEvery {
            weatherApiService.getCurrentWeatherByCity("Osaka", "api-key", "metric")
        } returns sampleWeatherResponse(cityName = "Osaka")

        val repository = WeatherRepository(
            context = context,
            secureStorage = secureStorage,
            weatherApiService = weatherApiService,
            networkAvailable = { true }
        )

        val results = repository.getWeatherByCity("Osaka").toList()

        assertEquals(2, results.size)
        val success = results[1] as WeatherResult.Success
        assertEquals("Osaka", success.data.location)
        assertEquals("Clear sky", success.data.condition)
        coVerify { weatherApiService.getCurrentWeatherByCity("Osaka", "api-key", "metric") }
    }

    @Test
    fun `getWeatherByCoordinates maps 401 to invalid api key`() = runTest {
        every { secureStorage.getWeatherApiKey() } returns "bad-key"
        coEvery {
            weatherApiService.getCurrentWeather(any(), any(), any(), any())
        } throws Exception("401 unauthorized")

        val repository = WeatherRepository(
            context = context,
            secureStorage = secureStorage,
            weatherApiService = weatherApiService,
            networkAvailable = { true }
        )

        val results = repository.getWeatherByCoordinates(35.0, 139.0).toList()

        assertEquals(
            listOf(WeatherResult.Loading, WeatherResult.Error("Invalid API key")),
            results
        )
    }

    @Test
    fun `getWeatherByCoordinates maps 404 to location not found`() = runTest {
        every { secureStorage.getWeatherApiKey() } returns "api-key"
        coEvery {
            weatherApiService.getCurrentWeather(any(), any(), any(), any())
        } throws Exception("404 not found")

        val repository = WeatherRepository(
            context = context,
            secureStorage = secureStorage,
            weatherApiService = weatherApiService,
            networkAvailable = { true }
        )

        val results = repository.getWeatherByCoordinates(35.0, 139.0).toList()

        assertEquals(
            listOf(WeatherResult.Loading, WeatherResult.Error("Location not found")),
            results
        )
    }

    @Test
    fun `getWeatherByCity maps 404 to city not found`() = runTest {
        every { secureStorage.getWeatherApiKey() } returns "api-key"
        coEvery {
            weatherApiService.getCurrentWeatherByCity(any(), any(), any())
        } throws Exception("404 not found")

        val repository = WeatherRepository(
            context = context,
            secureStorage = secureStorage,
            weatherApiService = weatherApiService,
            networkAvailable = { true }
        )

        val results = repository.getWeatherByCity("MissingCity").toList()

        assertEquals(
            listOf(WeatherResult.Loading, WeatherResult.Error("City not found")),
            results
        )
    }

    @Test
    fun `getWeatherByCity maps 429 to rate limit exceeded`() = runTest {
        every { secureStorage.getWeatherApiKey() } returns "api-key"
        coEvery {
            weatherApiService.getCurrentWeatherByCity(any(), any(), any())
        } throws Exception("429 too many requests")

        val repository = WeatherRepository(
            context = context,
            secureStorage = secureStorage,
            weatherApiService = weatherApiService,
            networkAvailable = { true }
        )

        val results = repository.getWeatherByCity("Tokyo").toList()

        assertEquals(
            listOf(WeatherResult.Loading, WeatherResult.Error("API rate limit exceeded")),
            results
        )
    }

    @Test
    fun `getWeatherByCoordinates maps generic failure to no internet when offline`() = runTest {
        every { secureStorage.getWeatherApiKey() } returns "api-key"
        coEvery {
            weatherApiService.getCurrentWeather(any(), any(), any(), any())
        } throws Exception("timeout")

        val repository = WeatherRepository(
            context = context,
            secureStorage = secureStorage,
            weatherApiService = weatherApiService,
            networkAvailable = { false }
        )

        val results = repository.getWeatherByCoordinates(35.0, 139.0).toList()

        assertEquals(
            listOf(WeatherResult.Loading, WeatherResult.Error("No internet connection")),
            results
        )
    }

    @Test
    fun `saveApiKey delegates to secure storage`() {
        every { secureStorage.saveWeatherApiKey("new-key") } just Runs

        val repository = WeatherRepository(
            context = context,
            secureStorage = secureStorage,
            weatherApiService = weatherApiService,
            networkAvailable = { true }
        )

        repository.saveApiKey("new-key")

        verify { secureStorage.saveWeatherApiKey("new-key") }
    }

    @Test
    fun `hasApiKey delegates to secure storage`() {
        every { secureStorage.hasWeatherApiKey() } returns true

        val repository = WeatherRepository(
            context = context,
            secureStorage = secureStorage,
            weatherApiService = weatherApiService,
            networkAvailable = { true }
        )

        assertTrue(repository.hasApiKey())
        verify { secureStorage.hasWeatherApiKey() }
    }

    @Test
    fun `removeApiKey delegates to secure storage`() {
        every { secureStorage.removeWeatherApiKey() } just Runs

        val repository = WeatherRepository(
            context = context,
            secureStorage = secureStorage,
            weatherApiService = weatherApiService,
            networkAvailable = { true }
        )

        repository.removeApiKey()

        verify { secureStorage.removeWeatherApiKey() }
    }

    private fun sampleWeatherResponse(cityName: String) = WeatherResponse(
        weather = listOf(
            Weather(
                id = 800,
                main = "Clear",
                description = "clear sky",
                icon = "01d"
            )
        ),
        main = MainWeatherData(
            temperature = 22.4,
            feelsLike = 24.1,
            tempMin = 20.2,
            tempMax = 25.8,
            pressure = 1012,
            humidity = 60
        ),
        wind = Wind(
            speed = 3.4,
            degree = 90
        ),
        clouds = Clouds(all = 8),
        visibility = 10000,
        timestamp = 1_711_111_111,
        sys = Sys(
            country = "JP",
            sunrise = 1_711_090_000,
            sunset = 1_711_140_000
        ),
        timezone = 32_400,
        cityId = 1_234,
        cityName = cityName,
        cod = 200
    )
}
