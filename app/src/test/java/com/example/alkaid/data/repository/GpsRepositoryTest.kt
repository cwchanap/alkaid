package com.example.alkaid.data.repository

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.data.sensor.SensorResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.mockk.every
import io.mockk.mockk

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE, sdk = [28])
class GpsRepositoryTest {

    private lateinit var context: Context
    private lateinit var gpsRepository: GpsRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocationTask: Task<Location>
    private var hasLocationPermission: Boolean = true

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fusedLocationClient = mockk(relaxed = true)
        lastLocationTask = mockk(relaxed = true)
        every { fusedLocationClient.lastLocation } returns lastLocationTask
        gpsRepository = GpsRepository(
            context,
            fusedLocationClient,
            hasLocationPermission = { hasLocationPermission }
        )
    }

    @Test
    fun `getSensorData when permission not granted should return error`() = runBlocking {
        // Given
        hasLocationPermission = false

        // When
        val result = gpsRepository.getSensorData()
            .filterNot { it is SensorResult.Loading }
            .first()

        // Then
        assert(result is SensorResult.Error)
        assertEquals("Location permission not granted", (result as SensorResult.Error).message)
    }

    @Test
    fun `getSensorData when location is successful should return data`() = runBlocking {
        // Given
        hasLocationPermission = true
        val location = Location(LocationManager.GPS_PROVIDER)
        location.latitude = 37.7749
        location.longitude = -122.4194
        location.altitude = 100.0
        location.accuracy = 10.0f
        every { lastLocationTask.addOnSuccessListener(any<OnSuccessListener<Location>>()) } answers {
            firstArg<OnSuccessListener<Location>>().onSuccess(location)
            lastLocationTask
        }
        every { lastLocationTask.addOnFailureListener(any()) } returns lastLocationTask

        // When
        val result = gpsRepository.getSensorData()
            .filterNot { it is SensorResult.Loading }
            .first()

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
        hasLocationPermission = true
        every { lastLocationTask.addOnSuccessListener(any<OnSuccessListener<Location>>()) } returns lastLocationTask
        every { lastLocationTask.addOnFailureListener(any<OnFailureListener>()) } answers {
            firstArg<OnFailureListener>().onFailure(Exception("Location unavailable"))
            lastLocationTask
        }

        // When
        val result = gpsRepository.getSensorData()
            .filterNot { it is SensorResult.Loading }
            .first()

        // Then
        assert(result is SensorResult.Error)
        assertEquals("Failed to get location: Location unavailable", (result as SensorResult.Error).message)
    }
}
