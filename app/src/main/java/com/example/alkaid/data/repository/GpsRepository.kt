package com.example.alkaid.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.data.sensor.SensorResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Repository for GPS location data.
 * Provides location updates including latitude, longitude, altitude, and accuracy.
 */
class GpsRepository(private val context: Context) : BaseSensorRepository<LocationData> {

    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)

    override fun getSensorData(): Flow<SensorResult> = callbackFlow {
        // Check for location permissions
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context, 
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context, 
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED && 
            coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            trySend(SensorResult.Error("Location permission not granted"))
            close()
            return@callbackFlow
        }

        trySend(SensorResult.Loading)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // Update interval: 10 seconds
        ).apply {
            setMinUpdateIntervalMillis(5000L) // Fastest update interval: 5 seconds
            setMaxUpdateDelayMillis(20000L) // Max delay: 20 seconds
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        accuracy = if (location.hasAccuracy()) location.accuracy else null
                    )
                    trySend(SensorResult.Data(locationData))
                }
            }
        }

        try {
            @SuppressLint("MissingPermission")
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            
            // Try to get last known location immediately
            @SuppressLint("MissingPermission")
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val locationData = LocationData(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        altitude = if (it.hasAltitude()) it.altitude else null,
                        accuracy = if (it.hasAccuracy()) it.accuracy else null
                    )
                    trySend(SensorResult.Data(locationData))
                }
            }.addOnFailureListener { exception ->
                trySend(SensorResult.Error("Failed to get location: ${exception.message}"))
            }
            
        } catch (e: SecurityException) {
            trySend(SensorResult.Error("Location permission denied"))
        } catch (e: Exception) {
            trySend(SensorResult.Error("GPS error: ${e.message}"))
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }.distinctUntilChanged()

    override suspend fun startListening() {
        // Implementation handled by the Flow
    }

    override suspend fun stopListening() {
        // Implementation handled by the Flow's awaitClose
    }
}
