package com.example.alkaid.data.preferences

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class MapPreferencesTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var sut: MapPreferences

    @Before
    fun setup() {
        context = mockk()
        prefs = mockk()
        editor = mockk(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putFloat(any(), any()) } returns editor
        sut = MapPreferences(context)
    }

    @Test
    fun `getProvider defaults to OSM when missing`() {
        every { prefs.getString(any(), any()) } returns null
        assertEquals(MapPreferences.MapProvider.OSM, sut.getProvider())
    }

    @Test
    fun `getProvider falls back to OSM on invalid value`() {
        every { prefs.getString(any(), any()) } returns "NOT_A_PROVIDER"
        assertEquals(MapPreferences.MapProvider.OSM, sut.getProvider())
    }

    @Test
    fun `setProvider writes enum name`() {
        sut.setProvider(MapPreferences.MapProvider.GOOGLE)
        verify { editor.putString("provider", MapPreferences.MapProvider.GOOGLE.name) }
        verify { editor.apply() }
    }

    @Test
    fun `default zoom get and set`() {
        every { prefs.getFloat("default_zoom", any()) } returns 9f
        assertEquals(9f, sut.getDefaultZoom())

        sut.setDefaultZoom(12.5f)
        verify { editor.putFloat("default_zoom", 12.5f) }
        verify { editor.apply() }
    }
}

