package com.example.alkaid.data.weather

import com.google.gson.annotations.SerializedName

/**
 * Main weather response from OpenWeatherMap API
 */
data class WeatherResponse(
    @SerializedName("weather")
    val weather: List<Weather>,
    @SerializedName("main")
    val main: MainWeatherData,
    @SerializedName("wind")
    val wind: Wind,
    @SerializedName("clouds")
    val clouds: Clouds,
    @SerializedName("visibility")
    val visibility: Int,
    @SerializedName("dt")
    val timestamp: Long,
    @SerializedName("sys")
    val sys: Sys,
    @SerializedName("timezone")
    val timezone: Int,
    @SerializedName("id")
    val cityId: Int,
    @SerializedName("name")
    val cityName: String,
    @SerializedName("cod")
    val cod: Int
)

/**
 * Weather condition information
 */
data class Weather(
    @SerializedName("id")
    val id: Int,
    @SerializedName("main")
    val main: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("icon")
    val icon: String
) {
    /**
     * Get user-friendly weather condition
     */
    fun getConditionText(): String = description.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase() else it.toString() 
    }
    
    /**
     * Get weather icon URL
     */
    fun getIconUrl(): String = "https://openweathermap.org/img/wn/$icon@2x.png"
}

/**
 * Main weather data (temperature, pressure, humidity)
 */
data class MainWeatherData(
    @SerializedName("temp")
    val temperature: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    @SerializedName("temp_min")
    val tempMin: Double,
    @SerializedName("temp_max")
    val tempMax: Double,
    @SerializedName("pressure")
    val pressure: Int,
    @SerializedName("humidity")
    val humidity: Int,
    @SerializedName("sea_level")
    val seaLevel: Int? = null,
    @SerializedName("grnd_level")
    val groundLevel: Int? = null
) {
    /**
     * Get formatted temperature in Celsius
     */
    fun getFormattedTemperature(): String = "${temperature.toInt()}°C"
    
    /**
     * Get formatted feels like temperature
     */
    fun getFormattedFeelsLike(): String = "Feels like ${feelsLike.toInt()}°C"
    
    /**
     * Get formatted temperature range
     */
    fun getFormattedTempRange(): String = "${tempMin.toInt()}°/${tempMax.toInt()}°"
    
    /**
     * Get formatted humidity
     */
    fun getFormattedHumidity(): String = "$humidity%"
    
    /**
     * Get formatted pressure
     */
    fun getFormattedPressure(): String = "$pressure hPa"
}

/**
 * Wind information
 */
data class Wind(
    @SerializedName("speed")
    val speed: Double,
    @SerializedName("deg")
    val degree: Int? = null,
    @SerializedName("gust")
    val gust: Double? = null
) {
    /**
     * Get formatted wind speed
     */
    fun getFormattedSpeed(): String = "${"%.1f".format(speed)} m/s"
    
    /**
     * Get wind direction
     */
    fun getDirection(): String {
        return degree?.let { deg ->
            when (deg) {
                in 0..22, in 338..360 -> "N"
                in 23..67 -> "NE"
                in 68..112 -> "E"
                in 113..157 -> "SE"
                in 158..202 -> "S"
                in 203..247 -> "SW"
                in 248..292 -> "W"
                in 293..337 -> "NW"
                else -> "N"
            }
        } ?: "N/A"
    }
    
    /**
     * Get formatted wind info
     */
    fun getFormattedWind(): String {
        val speedText = getFormattedSpeed()
        val directionText = getDirection()
        return "$speedText $directionText"
    }
}

/**
 * Cloud information
 */
data class Clouds(
    @SerializedName("all")
    val all: Int
) {
    /**
     * Get formatted cloudiness
     */
    fun getFormattedCloudiness(): String = "$all%"
}

/**
 * System information (sunrise, sunset, country)
 */
data class Sys(
    @SerializedName("type")
    val type: Int? = null,
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("country")
    val country: String,
    @SerializedName("sunrise")
    val sunrise: Long,
    @SerializedName("sunset")
    val sunset: Long
)

/**
 * Processed weather data for UI display
 */
data class WeatherDisplayData(
    val location: String,
    val temperature: String,
    val condition: String,
    val feelsLike: String,
    val tempRange: String,
    val humidity: String,
    val pressure: String,
    val windSpeed: String,
    val cloudiness: String,
    val visibility: String,
    val iconUrl: String,
    val lastUpdated: String
) {
    companion object {
        /**
         * Create display data from API response
         */
        fun fromWeatherResponse(response: WeatherResponse, locationName: String? = null): WeatherDisplayData {
            val weather = response.weather.firstOrNull()
            val main = response.main
            val wind = response.wind
            
            return WeatherDisplayData(
                location = locationName ?: response.cityName,
                temperature = main.getFormattedTemperature(),
                condition = weather?.getConditionText() ?: "Unknown",
                feelsLike = main.getFormattedFeelsLike(),
                tempRange = main.getFormattedTempRange(),
                humidity = main.getFormattedHumidity(),
                pressure = main.getFormattedPressure(),
                windSpeed = wind.getFormattedWind(),
                cloudiness = response.clouds.getFormattedCloudiness(),
                visibility = "${response.visibility / 1000} km",
                iconUrl = weather?.getIconUrl() ?: "",
                lastUpdated = formatTimestamp(response.timestamp)
            )
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val date = java.util.Date(timestamp * 1000)
            val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            return formatter.format(date)
        }
    }
}
