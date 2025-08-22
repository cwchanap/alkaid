package com.example.alkaid.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.alkaid.data.sensor.SensorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Manages sensor visibility preferences using SharedPreferences.
 * Provides reactive updates through Flow for UI components.
 */
class SensorVisibilityPreferences(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "sensor_visibility_prefs"
        private const val DEFAULT_VISIBILITY = true
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // StateFlow to track preferences changes
    private val _preferencesChanged = MutableStateFlow(0)
    
    /**
     * Gets the visibility state for a specific sensor type.
     */
    fun isSensorVisible(sensorType: SensorType): Boolean {
        return sharedPreferences.getBoolean(sensorType.settingsKey, DEFAULT_VISIBILITY)
    }
    
    /**
     * Sets the visibility state for a specific sensor type.
     */
    fun setSensorVisible(sensorType: SensorType, isVisible: Boolean) {
        // Avoid chaining so tests can verify apply() on the same mock instance
        val editor = sharedPreferences.edit()
        editor.putBoolean(sensorType.settingsKey, isVisible)
        editor.apply()

        // Trigger state change notification
        _preferencesChanged.value += 1
    }
    
    /**
     * Returns a Flow that emits the current visibility state for a sensor.
     * The Flow will emit whenever the preference changes.
     */
    fun getSensorVisibilityFlow(sensorType: SensorType): Flow<Boolean> {
        return _preferencesChanged.asStateFlow().map { 
            isSensorVisible(sensorType)
        }
    }
    
    /**
     * Returns a Flow that emits a list of currently visible sensor types.
     * Useful for adapters that need to show/hide sensors dynamically.
     */
    fun getVisibleSensorsFlow(): Flow<List<SensorType>> {
        return _preferencesChanged.asStateFlow().map {
            SensorType.values().filter { sensorType -> 
                isSensorVisible(sensorType) 
            }
        }
    }
    
    /**
     * Returns a map of all sensor visibility states.
     */
    fun getAllSensorVisibilityStates(): Map<SensorType, Boolean> {
        return SensorType.values().associateWith { sensorType ->
            isSensorVisible(sensorType)
        }
    }
    
    /**
     * Resets all sensor visibility preferences to default (visible).
     */
    fun resetToDefaults() {
        val editor = sharedPreferences.edit()
        SensorType.values().forEach { sensorType ->
            editor.putBoolean(sensorType.settingsKey, DEFAULT_VISIBILITY)
        }
        editor.apply()
        
        _preferencesChanged.value += 1
    }
}
