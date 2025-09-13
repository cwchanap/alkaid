
package com.example.alkaid.ui.constellation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alkaid.data.astronomy.Constellation
import com.example.alkaid.data.repository.ConstellationRepository
import com.example.alkaid.data.repository.GpsRepository
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.data.sensor.SensorResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ConstellationViewModel(application: Application) : AndroidViewModel(application) {

    private val constellationRepository = ConstellationRepository()
    private val gpsRepository = GpsRepository(application.applicationContext)

    private val _constellations = MutableStateFlow<List<Constellation>>(emptyList())
    val constellations: StateFlow<List<Constellation>> = _constellations.asStateFlow()

    private val _location = MutableStateFlow<LocationData?>(null)
    val location: StateFlow<LocationData?> = _location.asStateFlow()

    init {
        loadConstellations()
        observeLocation()
    }

    private fun loadConstellations() {
        _constellations.value = constellationRepository.getConstellations()
    }

    private fun observeLocation() {
        gpsRepository.getSensorData().onEach {
            if (it is SensorResult.Data<*>) {
                _location.value = it.value as? LocationData
            }
        }.launchIn(viewModelScope)
    }
}
