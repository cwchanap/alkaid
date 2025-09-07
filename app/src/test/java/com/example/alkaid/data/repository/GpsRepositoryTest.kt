package com.example.alkaid.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.data.sensor.SensorResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import io.mockk.*
import io.mockk.*
import org.robolectric.shadows.ShadowLocationManager

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE, sdk = [28])
class GpsRepositoryTest {

    private lateinit var context: Context
    private lateinit var gpsRepository: GpsRepository
    private lateinit var shadowLocationManager: ShadowLocationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        gpsRepository = GpsRepository(context)
        shadowLocationManager = Shadows.shadowOf(context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
    }

    @Test
    fun `getSensorData when permission not granted should return error`() = runBlocking {
        // Given
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, false)

        // When
        val result = gpsRepository.getSensorData().first()

        // Then
        assert(result is SensorResult.Error)
        assertEquals("Location permission not granted", (result as SensorResult.Error).message)
    }

    @Test
    fun `getSensorData when location is successful should return data`() = runBlocking {
        // Given
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val location = Location(LocationManager.GPS_PROVIDER)
        location.latitude = 37.7749
        location.longitude = -122.4194
        location.altitude = 100.0
        location.accuracy = 10.0f
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, location)

        // When
        val result = gpsRepository.getSensorData().first()

        // Then
        assert(result is SensorResult.Data<*>)
        val locationData = (result as SensorResult.Data<LocationData>).value
        assertEquals(37.7749, locationData.latitude, 0.0)
        assertEquals(-122.4194, locationData.longitude, 0.0)
        assertEquals(100.0, locationData.altitude!!, 0.0)
        assertEquals(10.0f, locationData.accuracy!!, 0.0f)
    }

    @Test
    fun `getSensorData when location fails should return error`() = runBlocking {
        // Given
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, null)

        // When
        val result = gpsRepository.getSensorData().first()

        // Then
        assert(result is SensorResult.Error)
        assertEquals("Failed to get location: last known location is null", (result as SensorResult.Error).message)
    }
}

