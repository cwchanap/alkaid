package com.example.alkaid.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.alkaid.R
import com.example.alkaid.data.preferences.SensorVisibilityPreferences
import com.example.alkaid.data.preferences.MapPreferences
import com.example.alkaid.data.preferences.MapPreferences.MapProvider
import com.example.alkaid.data.sensor.SensorType
import com.example.alkaid.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Fragment for managing sensor visibility settings.
 * Allows users to show/hide individual sensor widgets and handles location permissions.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorPreferences: SensorVisibilityPreferences
    private lateinit var mapPreferences: MapPreferences

    // Request permission launcher for location
    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted, GPS switch should remain checked
            binding.switchGps.isChecked = true
            sensorPreferences.setSensorVisible(SensorType.GPS, true)
        } else {
            // Permission denied, turn off GPS switch
            binding.switchGps.isChecked = false
            sensorPreferences.setSensorVisible(SensorType.GPS, false)
            
            // Show information about location permission
            showLocationPermissionDeniedInfo()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sensorPreferences = SensorVisibilityPreferences(requireContext())
        mapPreferences = MapPreferences(requireContext())
        
        setupSwitches()
        setupMapProvider()
        setupMapZoom()
        observePreferences()
    }

    private fun setupSwitches() {
        // Initialize switch states
        binding.switchBarometer.isChecked = sensorPreferences.isSensorVisible(SensorType.BAROMETER)
        binding.switchGyroscope.isChecked = sensorPreferences.isSensorVisible(SensorType.GYROSCOPE)
        binding.switchTemperature.isChecked = sensorPreferences.isSensorVisible(SensorType.TEMPERATURE)
        binding.switchGps.isChecked = sensorPreferences.isSensorVisible(SensorType.GPS)
        binding.switchAccelerometer.isChecked = sensorPreferences.isSensorVisible(SensorType.ACCELEROMETER)
        binding.switchMagnetometer.isChecked = sensorPreferences.isSensorVisible(SensorType.MAGNETOMETER)
        binding.switchLight.isChecked = sensorPreferences.isSensorVisible(SensorType.LIGHT)
        binding.switchHumidity.isChecked = sensorPreferences.isSensorVisible(SensorType.HUMIDITY)

        // Set up switch listeners
        binding.switchBarometer.setOnCheckedChangeListener { _, isChecked ->
            sensorPreferences.setSensorVisible(SensorType.BAROMETER, isChecked)
        }

        binding.switchGyroscope.setOnCheckedChangeListener { _, isChecked ->
            sensorPreferences.setSensorVisible(SensorType.GYROSCOPE, isChecked)
        }

        binding.switchTemperature.setOnCheckedChangeListener { _, isChecked ->
            sensorPreferences.setSensorVisible(SensorType.TEMPERATURE, isChecked)
        }

        binding.switchGps.setOnCheckedChangeListener { _, isChecked ->
            handleGpsToggle(isChecked)
        }

        binding.switchAccelerometer.setOnCheckedChangeListener { _, isChecked ->
            sensorPreferences.setSensorVisible(SensorType.ACCELEROMETER, isChecked)
        }

        binding.switchMagnetometer.setOnCheckedChangeListener { _, isChecked ->
            sensorPreferences.setSensorVisible(SensorType.MAGNETOMETER, isChecked)
        }

        binding.switchLight.setOnCheckedChangeListener { _, isChecked ->
            sensorPreferences.setSensorVisible(SensorType.LIGHT, isChecked)
        }

        binding.switchHumidity.setOnCheckedChangeListener { _, isChecked ->
            sensorPreferences.setSensorVisible(SensorType.HUMIDITY, isChecked)
        }
    }

    private fun setupMapZoom() {
        // Initialize slider from preference
        val current = mapPreferences.getDefaultZoom().toInt()
        binding.sliderDefaultZoom.value = current.toFloat()
        binding.txtDefaultZoomValue.text = getString(R.string.settings_map_zoom_value, current)

        binding.sliderDefaultZoom.addOnChangeListener { _, value, fromUser ->
            val zoom = value.toInt().coerceIn(3, 20)
            binding.txtDefaultZoomValue.text = getString(R.string.settings_map_zoom_value, zoom)
            mapPreferences.setDefaultZoom(zoom.toFloat())
        }
    }

    private fun setupMapProvider() {
        // Initialize selection from saved preference (default OSM)
        when (mapPreferences.getProvider()) {
            MapProvider.GOOGLE -> binding.toggleMapProviderSettings.check(binding.btnProviderGoogleSettings.id)
            MapProvider.OSM -> binding.toggleMapProviderSettings.check(binding.btnProviderOsmSettings.id)
        }

        binding.toggleMapProviderSettings.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                binding.btnProviderGoogleSettings.id -> mapPreferences.setProvider(MapProvider.GOOGLE)
                binding.btnProviderOsmSettings.id -> mapPreferences.setProvider(MapProvider.OSM)
            }
        }
    }

    private fun handleGpsToggle(isChecked: Boolean) {
        if (isChecked) {
            // Check if location permission is already granted
            val fineLocationGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarseLocationGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (fineLocationGranted || coarseLocationGranted) {
                // Permission already granted
                sensorPreferences.setSensorVisible(SensorType.GPS, true)
            } else {
                // Need to request permission
                showLocationPermissionRationale()
            }
        } else {
            // Turning off GPS sensor
            sensorPreferences.setSensorVisible(SensorType.GPS, false)
        }
    }

    private fun showLocationPermissionRationale() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Location Permission Required")
            .setMessage(getString(R.string.location_permission_required))
            .setPositiveButton("Grant Permission") { _, _ ->
                requestLocationPermission.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.switchGps.isChecked = false
            }
            .show()
    }

    private fun showLocationPermissionDeniedInfo() {
        Snackbar.make(
            binding.root,
            "Location permission is required for GPS sensor",
            Snackbar.LENGTH_LONG
        ).setAction("Settings") {
            // Open app settings
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
        }.show()
    }

    private fun observePreferences() {
        // Observe preference changes to keep switches in sync
        sensorPreferences.getSensorVisibilityFlow(SensorType.BAROMETER)
            .onEach { isVisible ->
                if (binding.switchBarometer.isChecked != isVisible) {
                    binding.switchBarometer.isChecked = isVisible
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        sensorPreferences.getSensorVisibilityFlow(SensorType.GYROSCOPE)
            .onEach { isVisible ->
                if (binding.switchGyroscope.isChecked != isVisible) {
                    binding.switchGyroscope.isChecked = isVisible
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        sensorPreferences.getSensorVisibilityFlow(SensorType.TEMPERATURE)
            .onEach { isVisible ->
                if (binding.switchTemperature.isChecked != isVisible) {
                    binding.switchTemperature.isChecked = isVisible
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        sensorPreferences.getSensorVisibilityFlow(SensorType.GPS)
            .onEach { isVisible ->
                if (binding.switchGps.isChecked != isVisible) {
                    binding.switchGps.isChecked = isVisible
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        sensorPreferences.getSensorVisibilityFlow(SensorType.ACCELEROMETER)
            .onEach { isVisible ->
                if (binding.switchAccelerometer.isChecked != isVisible) {
                    binding.switchAccelerometer.isChecked = isVisible
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        sensorPreferences.getSensorVisibilityFlow(SensorType.MAGNETOMETER)
            .onEach { isVisible ->
                if (binding.switchMagnetometer.isChecked != isVisible) {
                    binding.switchMagnetometer.isChecked = isVisible
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        sensorPreferences.getSensorVisibilityFlow(SensorType.LIGHT)
            .onEach { isVisible ->
                if (binding.switchLight.isChecked != isVisible) {
                    binding.switchLight.isChecked = isVisible
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        sensorPreferences.getSensorVisibilityFlow(SensorType.HUMIDITY)
            .onEach { isVisible ->
                if (binding.switchHumidity.isChecked != isVisible) {
                    binding.switchHumidity.isChecked = isVisible
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
