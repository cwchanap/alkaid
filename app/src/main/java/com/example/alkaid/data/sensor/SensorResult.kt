package com.example.alkaid.data.sensor

/**
 * Sealed interface representing the various states of sensor data.
 * This provides a unified way to handle loading, success, and error states
 * across all sensor types.
 */
sealed interface SensorResult {
    /**
     * Indicates that sensor data is being loaded/initialized.
     */
    object Loading : SensorResult

    /**
     * Contains successful sensor data.
     * @param T The type of sensor data (e.g., Float, LocationData, etc.)
     */
    data class Data<T>(val value: T) : SensorResult

    /**
     * Represents an error state with a descriptive message.
     */
    data class Error(val message: String) : SensorResult
}
