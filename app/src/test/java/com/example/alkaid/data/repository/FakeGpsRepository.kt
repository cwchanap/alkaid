package com.example.alkaid.data.repository

import android.content.Context
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.data.sensor.SensorResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeGpsRepository(context: Context) : GpsRepository(context) {

    private val sensorData = mutableListOf<SensorResult>()

    fun setSensorData(data: SensorResult) {
        sensorData.add(data)
    }

    override fun getSensorData(): Flow<SensorResult> {
        return flowOf(*sensorData.toTypedArray())
    }
}
