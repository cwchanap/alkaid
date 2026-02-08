package com.example.alkaid.ui.home

import com.example.alkaid.data.sensor.SensorResult
import com.example.alkaid.data.sensor.SensorType

data class HomeUiState(
    val gpsResult: SensorResult = SensorResult.Loading,
    val visibleSensors: List<SensorType> = emptyList()
)
