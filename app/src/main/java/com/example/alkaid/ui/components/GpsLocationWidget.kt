package com.example.alkaid.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.alkaid.R
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.data.sensor.SensorResult

class GpsLocationWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val altitudeText: TextView
    private val latitudeText: TextView
    private val longitudeText: TextView
    private val statusText: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.gps_location_widget, this, true)
        
        altitudeText = findViewById(R.id.altitude_text)
        latitudeText = findViewById(R.id.latitude_text)
        longitudeText = findViewById(R.id.longitude_text)
        statusText = findViewById(R.id.status_text)
    }

    fun updateLocation(sensorResult: SensorResult) {
        when (sensorResult) {
            is SensorResult.Loading -> {
                showLoading()
            }
            is SensorResult.Data<*> -> {
                val locationData = sensorResult.value as? LocationData
                if (locationData != null) {
                    showData(locationData)
                } else {
                    showError("Invalid location data")
                }
            }
            is SensorResult.Error -> {
                showError(sensorResult.message)
            }
        }
    }

    private fun showLoading() {
        altitudeText.text = "Loading..."
        latitudeText.text = "--"
        longitudeText.text = "--"
        statusText.text = "Getting location..."
        statusText.visibility = View.VISIBLE
    }

    private fun showData(locationData: LocationData) {
        // Format altitude prominently
        altitudeText.text = locationData.getFormattedAltitude()
        
        // Format coordinates with appropriate precision
        latitudeText.text = "%.6f°".format(locationData.latitude)
        longitudeText.text = "%.6f°".format(locationData.longitude)
        
        // Show accuracy if available
        val accuracy = locationData.accuracy
        if (accuracy != null) {
            statusText.text = "Accuracy: ${locationData.getFormattedAccuracy()}"
            statusText.visibility = View.VISIBLE
        } else {
            statusText.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        altitudeText.text = "N/A"
        latitudeText.text = "--"
        longitudeText.text = "--"
        statusText.text = message
        statusText.visibility = View.VISIBLE
    }
}
