package com.example.alkaid.data.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherDataTest {

    // ---- Weather ----

    @Test
    fun `getConditionText capitalizes first letter`() {
        val weather = Weather(800, "Clear", "clear sky", "01d")
        assertEquals("Clear sky", weather.getConditionText())
    }

    @Test
    fun `getConditionText leaves already capitalized text unchanged`() {
        val weather = Weather(500, "Rain", "Light Rain", "10d")
        assertEquals("Light Rain", weather.getConditionText())
    }

    // ---- MainWeatherData ----

    @Test
    fun `getFormattedTemperature truncates to whole degrees Celsius`() {
        // toInt() truncates toward zero, so 22.7 → 22 and 22.9 → 22 (not rounded up to 23)
        val main = MainWeatherData(22.9, 21.0, 18.0, 25.0, 1013, 60)
        assertEquals("22°C", main.getFormattedTemperature())
    }

    @Test
    fun `getFormattedFeelsLike returns feels like string`() {
        val main = MainWeatherData(22.7, 21.3, 18.0, 25.0, 1013, 60)
        assertEquals("Feels like 21°C", main.getFormattedFeelsLike())
    }

    @Test
    fun `getFormattedTempRange returns min and max`() {
        val main = MainWeatherData(22.0, 21.0, 18.9, 25.4, 1013, 60)
        assertEquals("18°/25°", main.getFormattedTempRange())
    }

    @Test
    fun `getFormattedHumidity returns percentage`() {
        val main = MainWeatherData(22.0, 21.0, 18.0, 25.0, 1013, 65)
        assertEquals("65%", main.getFormattedHumidity())
    }

    @Test
    fun `getFormattedPressure returns hPa`() {
        val main = MainWeatherData(22.0, 21.0, 18.0, 25.0, 1015, 60)
        assertEquals("1015 hPa", main.getFormattedPressure())
    }

    // ---- Wind ----

    @Test
    fun `getFormattedSpeed formats to one decimal place`() {
        val wind = Wind(7.5)
        assertEquals("7.5 m/s", wind.getFormattedSpeed())
    }

    @Test
    fun `getDirection returns N for 0 degrees`() {
        val wind = Wind(5.0, 0)
        assertEquals("N", wind.getDirection())
    }

    @Test
    fun `getDirection returns N for 360 degrees`() {
        val wind = Wind(5.0, 360)
        assertEquals("N", wind.getDirection())
    }

    @Test
    fun `getDirection returns NE for 45 degrees`() {
        val wind = Wind(5.0, 45)
        assertEquals("NE", wind.getDirection())
    }

    @Test
    fun `getDirection returns E for 90 degrees`() {
        val wind = Wind(5.0, 90)
        assertEquals("E", wind.getDirection())
    }

    @Test
    fun `getDirection returns SE for 135 degrees`() {
        val wind = Wind(5.0, 135)
        assertEquals("SE", wind.getDirection())
    }

    @Test
    fun `getDirection returns S for 180 degrees`() {
        val wind = Wind(5.0, 180)
        assertEquals("S", wind.getDirection())
    }

    @Test
    fun `getDirection returns SW for 225 degrees`() {
        val wind = Wind(5.0, 225)
        assertEquals("SW", wind.getDirection())
    }

    @Test
    fun `getDirection returns W for 270 degrees`() {
        val wind = Wind(5.0, 270)
        assertEquals("W", wind.getDirection())
    }

    @Test
    fun `getDirection returns NW for 315 degrees`() {
        val wind = Wind(5.0, 315)
        assertEquals("NW", wind.getDirection())
    }

    @Test
    fun `getDirection returns N_A when degree is null`() {
        val wind = Wind(5.0, null)
        assertEquals("N/A", wind.getDirection())
    }

    @Test
    fun `getFormattedWind combines speed and direction`() {
        val wind = Wind(5.0, 90)
        assertEquals("5.0 m/s E", wind.getFormattedWind())
    }

    // ---- Clouds ----

    @Test
    fun `getFormattedCloudiness returns percentage`() {
        val clouds = Clouds(75)
        assertEquals("75%", clouds.getFormattedCloudiness())
    }

    // ---- WeatherDisplayData ----

    @Test
    fun `fromWeatherResponse creates display data with city name when no override`() {
        val response = buildWeatherResponse(cityName = "London")
        val displayData = WeatherDisplayData.fromWeatherResponse(response)
        assertEquals("London", displayData.location)
    }

    @Test
    fun `fromWeatherResponse uses locationName override when provided`() {
        val response = buildWeatherResponse(cityName = "London")
        val displayData = WeatherDisplayData.fromWeatherResponse(response, "51.5074°, -0.1278°")
        assertEquals("51.5074°, -0.1278°", displayData.location)
    }

    @Test
    fun `fromWeatherResponse maps temperature correctly`() {
        val response = buildWeatherResponse(temperature = 15.9)
        val displayData = WeatherDisplayData.fromWeatherResponse(response)
        assertEquals("15°C", displayData.temperature)
    }

    @Test
    fun `fromWeatherResponse maps condition correctly`() {
        val response = buildWeatherResponse(description = "light rain")
        val displayData = WeatherDisplayData.fromWeatherResponse(response)
        assertEquals("Light rain", displayData.condition)
    }

    @Test
    fun `fromWeatherResponse maps visibility in km`() {
        val response = buildWeatherResponse(visibility = 10000)
        val displayData = WeatherDisplayData.fromWeatherResponse(response)
        assertEquals("10 km", displayData.visibility)
    }

    @Test
    fun `fromWeatherResponse sets Unknown condition when weather list is empty`() {
        val response = buildWeatherResponse().copy(weather = emptyList())
        val displayData = WeatherDisplayData.fromWeatherResponse(response)
        assertEquals("Unknown", displayData.condition)
    }

    // ---- helpers ----

    private fun buildWeatherResponse(
        cityName: String = "TestCity",
        temperature: Double = 20.0,
        description: String = "clear sky",
        visibility: Int = 10000
    ): WeatherResponse {
        return WeatherResponse(
            weather = listOf(Weather(800, "Clear", description, "01d")),
            main = MainWeatherData(temperature, temperature - 1, temperature - 3, temperature + 3, 1013, 55),
            wind = Wind(3.0, 90),
            clouds = Clouds(10),
            visibility = visibility,
            timestamp = 1_700_000_000L,
            sys = Sys(country = "GB", sunrise = 1_700_000_000L, sunset = 1_700_050_000L),
            timezone = 0,
            cityId = 1,
            cityName = cityName,
            cod = 200
        )
    }
}
