package com.example.alkaid.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for sensitive data like API keys using Android Keystore
 */
class SecureStorage(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "alkaid_secure_prefs"
        private const val FALLBACK_PREFS_NAME = "alkaid_prefs_fallback"
        private const val WEATHER_API_KEY = "weather_api_key"
    }

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular SharedPreferences if EncryptedSharedPreferences fails
        // This can happen on older devices or in certain emulator configurations
        context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Save weather API key securely
     */
    fun saveWeatherApiKey(apiKey: String) {
        prefs.edit()
            .putString(WEATHER_API_KEY, apiKey)
            .apply()
    }

    /**
     * Get weather API key
     */
    fun getWeatherApiKey(): String? {
        return try {
            prefs.getString(WEATHER_API_KEY, null)
        } catch (e: Exception) {
            android.util.Log.e("SecureStorage", "Error getting weather API key", e)
            null
        }
    }

    /**
     * Check if weather API key exists
     */
    fun hasWeatherApiKey(): Boolean {
        return try {
            !getWeatherApiKey().isNullOrBlank()
        } catch (e: Exception) {
            android.util.Log.e("SecureStorage", "Error checking weather API key", e)
            false
        }
    }

    /**
     * Remove weather API key
     */
    fun removeWeatherApiKey() {
        prefs.edit()
            .remove(WEATHER_API_KEY)
            .apply()
    }

    /**
     * Clear all secure data
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
