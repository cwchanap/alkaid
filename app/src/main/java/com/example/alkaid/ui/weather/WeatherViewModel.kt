package com.example.alkaid.ui.weather

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alkaid.data.repository.GpsRepository
import com.example.alkaid.data.repository.WeatherRepository
import com.example.alkaid.data.repository.WeatherResult
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.data.sensor.SensorResult
import com.example.alkaid.data.weather.WeatherDisplayData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for the Weather fragment that manages weather data and location integration
 */
class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val weatherRepository = WeatherRepository(application.applicationContext)
    private val gpsRepository = GpsRepository(application.applicationContext)

    // Weather state
    private val _weatherState = MutableStateFlow<WeatherViewState>(WeatherViewState.CheckingApiKey)
    val weatherState: StateFlow<WeatherViewState> = _weatherState.asStateFlow()

    // Current location
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()

    // API key validation state
    private val _apiKeyValid = MutableStateFlow(false)
    val apiKeyValid: StateFlow<Boolean> = _apiKeyValid.asStateFlow()

    init {
        try {
            checkApiKeyAndInitialize()
            observeLocation()
        } catch (e: Exception) {
            _weatherState.value = WeatherViewState.Error("Initialization error: ${e.message}")
        }
    }

    /**
     * Check if API key exists and initialize accordingly
     */
    private fun checkApiKeyAndInitialize() {
        if (weatherRepository.hasApiKey()) {
            _apiKeyValid.value = true
            _weatherState.value = WeatherViewState.WaitingForLocation
        } else {
            _apiKeyValid.value = false
            _weatherState.value = WeatherViewState.NoApiKey
        }
    }

    /**
     * Start observing GPS location updates
     */
    private fun observeLocation() {
        gpsRepository.getSensorData()
            .onEach { sensorResult ->
                when (sensorResult) {
                    is SensorResult.Data<*> -> {
                        val locationData = sensorResult.value as LocationData
                        _currentLocation.value = locationData
                        
                        // Auto-fetch weather when location is available and API key exists
                        if (_apiKeyValid.value) {
                            fetchWeatherForLocation(locationData)
                        }
                    }
                    is SensorResult.Error -> {
                        if (_apiKeyValid.value) {
                            _weatherState.value = WeatherViewState.Error("Location error: ${sensorResult.message}")
                        }
                    }
                    is SensorResult.Loading -> {
                        if (_apiKeyValid.value && _weatherState.value == WeatherViewState.WaitingForLocation) {
                            _weatherState.value = WeatherViewState.WaitingForLocation
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Fetch weather data for the given location
     */
    private fun fetchWeatherForLocation(location: LocationData) {
        viewModelScope.launch {
            weatherRepository.getWeatherByCoordinates(location.latitude, location.longitude)
                .collect { result ->
                    _weatherState.value = when (result) {
                        is WeatherResult.Loading -> WeatherViewState.Loading
                        is WeatherResult.Success -> WeatherViewState.Success(result.data)
                        is WeatherResult.Error -> WeatherViewState.Error(result.message)
                        is WeatherResult.NoApiKey -> {
                            _apiKeyValid.value = false
                            WeatherViewState.NoApiKey
                        }
                    }
                }
        }
    }

    /**
     * Manually refresh weather data
     */
    fun refreshWeather() {
        val location = _currentLocation.value
        if (location != null && _apiKeyValid.value) {
            fetchWeatherForLocation(location)
        } else if (!_apiKeyValid.value) {
            _weatherState.value = WeatherViewState.NoApiKey
        } else {
            _weatherState.value = WeatherViewState.WaitingForLocation
        }
    }

    /**
     * Save API key and refresh weather data
     */
    fun saveApiKey(apiKey: String) {
        if (apiKey.isNotBlank()) {
            weatherRepository.saveApiKey(apiKey)
            _apiKeyValid.value = true
            
            // Try to fetch weather immediately if we have location
            val location = _currentLocation.value
            if (location != null) {
                fetchWeatherForLocation(location)
            } else {
                _weatherState.value = WeatherViewState.WaitingForLocation
            }
        }
    }

    /**
     * Remove API key
     */
    fun removeApiKey() {
        weatherRepository.removeApiKey()
        _apiKeyValid.value = false
        _weatherState.value = WeatherViewState.NoApiKey
    }

    /**
     * Check if API key is configured
     */
    fun hasApiKey(): Boolean {
        return weatherRepository.hasApiKey()
    }

    /**
     * Get formatted location string
     */
    fun getLocationString(): String {
        return _currentLocation.value?.getFormattedLatLng() ?: "Unknown location"
    }
}

/**
 * Sealed class representing different states of the weather view
 */
sealed class WeatherViewState {
    object CheckingApiKey : WeatherViewState()
    object NoApiKey : WeatherViewState()
    object WaitingForLocation : WeatherViewState()
    object Loading : WeatherViewState()
    data class Success(val weatherData: WeatherDisplayData) : WeatherViewState()
    data class Error(val message: String) : WeatherViewState()
}
