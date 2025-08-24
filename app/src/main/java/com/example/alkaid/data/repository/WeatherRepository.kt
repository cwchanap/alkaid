package com.example.alkaid.data.repository

import android.content.Context
import com.example.alkaid.data.security.SecureStorage
import com.example.alkaid.data.weather.WeatherApiService
import com.example.alkaid.data.weather.WeatherDisplayData
import com.example.alkaid.data.weather.WeatherResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Repository for weather data from OpenWeatherMap API
 */
class WeatherRepository(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    }

    private val secureStorage = SecureStorage(context)
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val weatherApiService = retrofit.create(WeatherApiService::class.java)

    /**
     * Get weather data by coordinates
     */
    fun getWeatherByCoordinates(
        latitude: Double,
        longitude: Double
    ): Flow<WeatherResult> = flow {
        emit(WeatherResult.Loading)
        
        try {
            val apiKey = secureStorage.getWeatherApiKey()
            if (apiKey.isNullOrBlank()) {
                emit(WeatherResult.NoApiKey)
                return@flow
            }

            val response = weatherApiService.getCurrentWeather(
                latitude = latitude,
                longitude = longitude,
                apiKey = apiKey
            )
            
            // Format location name from coordinates
            val locationName = "%.4f°, %.4f°".format(latitude, longitude)
            val displayData = WeatherDisplayData.fromWeatherResponse(response, locationName)
            emit(WeatherResult.Success(displayData))
            
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("401") == true -> "Invalid API key"
                e.message?.contains("404") == true -> "Location not found"
                e.message?.contains("429") == true -> "API rate limit exceeded"
                !isNetworkAvailable() -> "No internet connection"
                else -> "Failed to get weather data: ${e.message}"
            }
            emit(WeatherResult.Error(errorMessage))
        }
    }

    /**
     * Get weather data by city name
     */
    fun getWeatherByCity(cityName: String): Flow<WeatherResult> = flow {
        emit(WeatherResult.Loading)
        
        try {
            val apiKey = secureStorage.getWeatherApiKey()
            if (apiKey.isNullOrBlank()) {
                emit(WeatherResult.NoApiKey)
                return@flow
            }

            val response = weatherApiService.getCurrentWeatherByCity(
                cityName = cityName,
                apiKey = apiKey
            )
            
            val displayData = WeatherDisplayData.fromWeatherResponse(response)
            emit(WeatherResult.Success(displayData))
            
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("401") == true -> "Invalid API key"
                e.message?.contains("404") == true -> "City not found"
                e.message?.contains("429") == true -> "API rate limit exceeded"
                !isNetworkAvailable() -> "No internet connection"
                else -> "Failed to get weather data: ${e.message}"
            }
            emit(WeatherResult.Error(errorMessage))
        }
    }

    /**
     * Save API key securely
     */
    fun saveApiKey(apiKey: String) {
        secureStorage.saveWeatherApiKey(apiKey)
    }

    /**
     * Check if API key exists
     */
    fun hasApiKey(): Boolean {
        return secureStorage.hasWeatherApiKey()
    }

    /**
     * Remove API key
     */
    fun removeApiKey() {
        secureStorage.removeWeatherApiKey()
    }

    /**
     * Simple network availability check
     */
    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
                    as android.net.ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Sealed class representing different states of weather data
 */
sealed class WeatherResult {
    object Loading : WeatherResult()
    object NoApiKey : WeatherResult()
    data class Success(val data: WeatherDisplayData) : WeatherResult()
    data class Error(val message: String) : WeatherResult()
}
