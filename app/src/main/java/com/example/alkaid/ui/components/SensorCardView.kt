package com.example.alkaid.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.example.alkaid.R
import com.example.alkaid.data.sensor.GyroscopeData
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.data.sensor.SensorResult
import com.example.alkaid.data.sensor.SensorType
import com.google.android.material.card.MaterialCardView

/**
 * Custom view component for displaying sensor data in a Material Card.
 * Handles different sensor states (loading, data, error) and formats values appropriately.
 */
class SensorCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val sensorIcon: ImageView
    private val sensorTitle: TextView
    private val sensorValue: TextView
    private val sensorStatus: TextView
    private val errorBackground: View

    init {
        LayoutInflater.from(context).inflate(R.layout.sensor_card_view, this, true)
        
        sensorIcon = findViewById(R.id.sensor_icon)
        sensorTitle = findViewById(R.id.sensor_title)
        sensorValue = findViewById(R.id.sensor_value)
        sensorStatus = findViewById(R.id.sensor_status)
        errorBackground = findViewById(R.id.error_background)
    }

    /**
     * Sets up the card for a specific sensor type with icon and title.
     */
    fun setupSensor(sensorType: SensorType) {
        sensorTitle.text = sensorType.displayName
        
        val iconRes = when (sensorType) {
            SensorType.BAROMETER -> R.drawable.ic_dashboard_black_24dp // Using existing icon as placeholder
            SensorType.GYROSCOPE -> R.drawable.ic_home_black_24dp // Using existing icon as placeholder
            SensorType.TEMPERATURE -> R.drawable.ic_notifications_black_24dp // Using existing icon as placeholder
            SensorType.GPS -> R.drawable.ic_settings_black_24dp // Using existing icon as placeholder
        }
        
        sensorIcon.setImageResource(iconRes)
    }

    /**
     * Updates the card with the latest sensor result.
     */
    fun updateSensorResult(result: SensorResult) {
        when (result) {
            is SensorResult.Loading -> showLoading()
            is SensorResult.Data<*> -> showData(result.value)
            is SensorResult.Error -> showError(result.message)
        }
    }

    private fun showLoading() {
        sensorValue.text = context.getString(R.string.sensor_loading)
        sensorStatus.visibility = View.GONE
        errorBackground.visibility = View.GONE
        
        // Reset card appearance
        strokeColor = context.getColor(android.R.color.darker_gray)
    }

    private fun showData(value: Any?) {
        sensorStatus.visibility = View.GONE
        errorBackground.visibility = View.GONE
        
        sensorValue.text = when (value) {
            is Float -> formatFloatValue(value)
            is LocationData -> formatLocationData(value)
            is GyroscopeData -> value.getFormattedMagnitude()
            else -> value.toString()
        }
        
        // Reset card appearance
        strokeColor = context.getColor(android.R.color.darker_gray)
    }

    private fun showError(message: String) {
        sensorValue.text = context.getString(R.string.sensor_not_available)
        sensorStatus.text = message
        sensorStatus.visibility = View.VISIBLE
        errorBackground.visibility = View.VISIBLE
        
        // Update card appearance for error state
        strokeColor = context.getColor(android.R.color.holo_red_light)
    }

    private fun formatFloatValue(value: Float): String {
        return when {
            // Pressure values (typically 800-1200 hPa)
            value in 800f..1200f -> "%.1f hPa".format(value)
            // Temperature values (typically -50 to 60°C)
            value in -50f..60f -> "%.1f°C".format(value)
            // Generic float formatting
            else -> "%.2f".format(value)
        }
    }

    private fun formatLocationData(locationData: LocationData): String {
        return buildString {
            append(locationData.getFormattedLatLng())
            if (locationData.altitude != null) {
                append("\n${locationData.getFormattedAltitude()}")
            }
        }
    }

    /**
     * Sets a custom icon for the sensor card.
     */
    fun setIcon(@DrawableRes iconRes: Int) {
        sensorIcon.setImageResource(iconRes)
    }

    /**
     * Sets additional status information (useful for showing accuracy, etc.).
     */
    fun setStatusText(status: String?) {
        if (status.isNullOrEmpty()) {
            sensorStatus.visibility = View.GONE
        } else {
            sensorStatus.text = status
            sensorStatus.visibility = View.VISIBLE
            sensorStatus.setTextColor(context.getColor(android.R.color.darker_gray))
        }
    }
}
