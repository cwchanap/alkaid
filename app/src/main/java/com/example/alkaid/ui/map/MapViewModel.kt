package com.example.alkaid.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alkaid.data.repository.GpsRepository
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.data.sensor.SensorResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * ViewModel for the Map fragment that manages location data and map interactions.
 */
class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val gpsRepository = GpsRepository(application.applicationContext)

    // Current location state
    private val _locationState = MutableStateFlow<LocationState>(LocationState.Loading)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    // Map ready state
    private val _isMapReady = MutableStateFlow(false)
    val isMapReady: StateFlow<Boolean> = _isMapReady.asStateFlow()

    init {
        startLocationUpdates()
    }

    /**
     * Start observing GPS location updates
     */
    private fun startLocationUpdates() {
        gpsRepository.getSensorData()
            .onEach { sensorResult ->
                _locationState.value = when (sensorResult) {
                    is SensorResult.Loading -> LocationState.Loading
                    is SensorResult.Data<*> -> {
                        val locationData = sensorResult.value as LocationData
                        LocationState.Success(locationData)
                    }
                    is SensorResult.Error -> LocationState.Error(sensorResult.message)
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Called when the map is ready to be used
     */
    fun onMapReady() {
        _isMapReady.value = true
    }

    /**
     * Get current location data if available
     */
    fun getCurrentLocation(): LocationData? {
        return when (val state = _locationState.value) {
            is LocationState.Success -> state.locationData
            else -> null
        }
    }

    /**
     * Request a fresh location update
     */
    fun requestLocationUpdate() {
        // The repository is already providing continuous updates
        // This could be used to trigger immediate location request if needed
    }
}

/**
 * Sealed class representing different states of location data
 */
sealed class LocationState {
    object Loading : LocationState()
    data class Success(val locationData: LocationData) : LocationState()
    data class Error(val message: String) : LocationState()
}
