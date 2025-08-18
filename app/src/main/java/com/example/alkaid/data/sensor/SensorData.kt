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
 * Data class representing accelerometer data.
 */
data class AccelerometerData(
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun getMagnitude(): Float = kotlin.math.sqrt(x * x + y * y + z * z)
    fun getFormattedMagnitude(): String = "%.3f m/s²".format(getMagnitude())
}

/**
 * Data class representing magnetometer data.
 */
data class MagnetometerData(
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun getMagnitude(): Float = kotlin.math.sqrt(x * x + y * y + z * z)
    fun getFormattedMagnitude(): String = "%.2f µT".format(getMagnitude())
}

/**
 * Data class representing light sensor data.
 */
data class LightData(
    val illuminance: Float
) {
    fun getFormattedIlluminance(): String = "%.1f lx".format(illuminance)
}

/**
 * Data class representing humidity sensor data.
 */
data class HumidityData(
    val humidity: Float
) {
    fun getFormattedHumidity(): String = "%.1f%%".format(humidity)
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
    GPS("GPS Location", "°", "show_gps"),
    ACCELEROMETER("Accelerometer", "m/s²", "show_accelerometer"),
    MAGNETOMETER("Magnetometer", "µT", "show_magnetometer"),
    LIGHT("Light Sensor", "lx", "show_light"),
    HUMIDITY("Humidity", "%", "show_humidity");

    companion object {
        fun fromSettingsKey(key: String): SensorType? = values().find { it.settingsKey == key }
    }
}
