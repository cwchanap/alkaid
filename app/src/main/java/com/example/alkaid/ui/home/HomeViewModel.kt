package com.example.alkaid.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alkaid.data.preferences.SensorVisibilityPreferences
import com.example.alkaid.data.repository.AccelerometerRepository
import com.example.alkaid.data.repository.BarometerRepository
import com.example.alkaid.data.repository.BaseSensorRepository
import com.example.alkaid.data.repository.GpsRepository
import com.example.alkaid.data.repository.GyroscopeRepository
import com.example.alkaid.data.repository.HumiditySensorRepository
import com.example.alkaid.data.repository.LightSensorRepository
import com.example.alkaid.data.repository.MagnetometerRepository
import com.example.alkaid.data.repository.TemperatureRepository
import com.example.alkaid.data.sensor.SensorResult
import com.example.alkaid.data.sensor.SensorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    internal var sensorRepositories: Map<SensorType, BaseSensorRepository<*>> = mapOf(
        SensorType.BAROMETER to BarometerRepository(application.applicationContext),
        SensorType.GYROSCOPE to GyroscopeRepository(application.applicationContext),
        SensorType.TEMPERATURE to TemperatureRepository(application.applicationContext),
        SensorType.GPS to GpsRepository(application.applicationContext),
        SensorType.ACCELEROMETER to AccelerometerRepository(application.applicationContext),
        SensorType.MAGNETOMETER to MagnetometerRepository(application.applicationContext),
        SensorType.LIGHT to LightSensorRepository(application.applicationContext),
        SensorType.HUMIDITY to HumiditySensorRepository(application.applicationContext),
    )

    internal var sensorPreferences = SensorVisibilityPreferences(application.applicationContext)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var gpsObservationStarted = false

    fun startObserving() {
        observeVisibleSensors()
        observeGps()
    }

    private fun observeVisibleSensors() {
        sensorPreferences.getVisibleSensorsFlow()
            .onEach { visibleSensors ->
                _uiState.value = _uiState.value.copy(
                    visibleSensors = visibleSensors.filter { it != SensorType.GPS }
                )
            }
            .launchIn(viewModelScope)
    }

    private fun observeGps() {
        if (gpsObservationStarted) return
        gpsObservationStarted = true

        val gpsRepo = sensorRepositories[SensorType.GPS] ?: return
        gpsRepo.getSensorData()
            .onEach { result ->
                _uiState.value = _uiState.value.copy(gpsResult = result)
            }
            .launchIn(viewModelScope)
    }
}
