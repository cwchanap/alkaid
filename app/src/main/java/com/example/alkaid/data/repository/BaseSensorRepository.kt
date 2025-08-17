package com.example.alkaid.data.repository

import com.example.alkaid.data.sensor.SensorResult
import kotlinx.coroutines.flow.Flow

/**
 * Base interface for all sensor repositories.
 * Provides a common contract for sensor data access with reactive Flow-based updates.
 */
interface BaseSensorRepository<T> {
    
    /**
     * Returns a Flow that emits sensor data updates.
     * The Flow will emit:
     * - [SensorResult.Loading] initially
     * - [SensorResult.Data] when sensor data is available
     * - [SensorResult.Error] if the sensor is not available or encounters an error
     */
    fun getSensorData(): Flow<SensorResult>
    
    /**
     * Starts listening to sensor updates.
     * Should be called when the UI needs sensor data.
     */
    suspend fun startListening()
    
    /**
     * Stops listening to sensor updates.
     * Should be called when the UI no longer needs sensor data to conserve battery.
     */
    suspend fun stopListening()
}
