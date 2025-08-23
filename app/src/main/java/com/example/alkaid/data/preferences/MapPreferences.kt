package com.example.alkaid.data.preferences

import android.content.Context

class MapPreferences(context: Context) {

    enum class MapProvider { GOOGLE, OSM }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getProvider(): MapProvider {
        val name = prefs.getString(KEY_PROVIDER, MapProvider.OSM.name) ?: MapProvider.OSM.name
        return runCatching { MapProvider.valueOf(name) }.getOrDefault(MapProvider.OSM)
    }

    fun setProvider(provider: MapProvider) {
        prefs.edit().putString(KEY_PROVIDER, provider.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "map_preferences"
        private const val KEY_PROVIDER = "provider"
    }
}

