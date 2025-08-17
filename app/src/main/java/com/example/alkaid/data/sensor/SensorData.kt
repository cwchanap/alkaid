package com.example.alkaid.data.sensor

/**
 * Data class representing GPS location information.
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null
) {
    fun getFormattedLatLng(): String = "%.6f°, %.6f°".format(latitude, longitude)
    fun getFormattedAltitude(): String = altitude?.let { "%.1f m".format(it) } ?: "N/A"
    fun getFormattedAccuracy(): String = accuracy?.let { "±%.1f m".format(it) } ?: "N/A"
}

/**
 * Data class representing gyroscope rotation data.
 */
data class GyroscopeData(
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun getMagnitude(): Float = kotlin.math.sqrt(x * x + y * y + z * z)
    fun getFormattedMagnitude(): String = "%.3f rad/s".format(getMagnitude())
}

/**
 * Enum representing different sensor types for identification and settings.
 */
enum class SensorType(
    val displayName: String,
    val unit: String,
    val settingsKey: String
) {
    BAROMETER("Barometer", "hPa", "show_barometer"),
    GYROSCOPE("Gyroscope", "rad/s", "show_gyroscope"),
    TEMPERATURE("Temperature", "°C", "show_temperature"),
    GPS("GPS Location", "°", "show_gps");

    companion object {
        fun fromSettingsKey(key: String): SensorType? = values().find { it.settingsKey == key }
    }
}
