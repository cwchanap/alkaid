package com.example.alkaid.ui.map

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.alkaid.R
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Fragment that displays a Google Map with current location tracking
 */
class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MapViewModel
    private var googleMap: GoogleMap? = null
    private var currentLocationMarker: com.google.android.gms.maps.model.Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[MapViewModel::class.java]

        setupMap()
        setupObservers()
        setupFab()
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun setupObservers() {
        // Observe location state changes
        viewModel.locationState
            .onEach { locationState ->
                updateLocationUI(locationState)
                
                when (locationState) {
                    is LocationState.Loading -> {
                        binding.loadingProgress.visibility = View.VISIBLE
                        binding.locationInfoCard.visibility = View.GONE
                    }
                    is LocationState.Success -> {
                        binding.loadingProgress.visibility = View.GONE
                        binding.locationInfoCard.visibility = View.VISIBLE
                        updateMapLocation(locationState.locationData)
                    }
                    is LocationState.Error -> {
                        binding.loadingProgress.visibility = View.GONE
                        binding.locationInfoCard.visibility = View.VISIBLE
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setupFab() {
        binding.fabMyLocation.setOnClickListener {
            centerMapOnCurrentLocation()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Configure map settings
        map.apply {
            uiSettings.apply {
                isZoomControlsEnabled = true
                isCompassEnabled = true
                isMyLocationButtonEnabled = false // We use our own FAB
                isMapToolbarEnabled = true
            }
            
            // Try to enable the My Location layer if we have permission
            try {
                isMyLocationEnabled = true
            } catch (e: SecurityException) {
                // Permission not granted, handled by the repository
            }
        }
        
        viewModel.onMapReady()
        
        // If we already have location data, update the map
        viewModel.getCurrentLocation()?.let { locationData ->
            updateMapLocation(locationData)
        }
    }

    private fun updateLocationUI(locationState: LocationState) {
        when (locationState) {
            is LocationState.Loading -> {
                binding.locationCoordinates.text = getString(R.string.sensor_loading)
                binding.locationAccuracy.visibility = View.GONE
            }
            is LocationState.Success -> {
                val locationData = locationState.locationData
                binding.locationCoordinates.text = locationData.getFormattedLatLng()
                
                locationData.accuracy?.let { accuracy ->
                    binding.locationAccuracy.apply {
                        text = "Accuracy: ${locationData.getFormattedAccuracy()}"
                        visibility = View.VISIBLE
                    }
                } ?: run {
                    binding.locationAccuracy.visibility = View.GONE
                }
            }
            is LocationState.Error -> {
                binding.locationCoordinates.text = locationState.message
                binding.locationAccuracy.visibility = View.GONE
            }
        }
    }

    private fun updateMapLocation(locationData: LocationData) {
        googleMap?.let { map ->
            val latLng = LatLng(locationData.latitude, locationData.longitude)
            
            // Remove existing marker
            currentLocationMarker?.remove()
            
            // Add new marker
            currentLocationMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.map_my_location))
                    .snippet("${locationData.getFormattedLatLng()}\nAltitude: ${locationData.getFormattedAltitude()}")
            )
            
            // Move camera to location if this is the first location update
            if (!::viewModel.isInitialized || viewModel.isMapReady.value) {
                centerMapOnLocation(latLng)
            }
        }
    }

    private fun centerMapOnCurrentLocation() {
        viewModel.getCurrentLocation()?.let { locationData ->
            val latLng = LatLng(locationData.latitude, locationData.longitude)
            centerMapOnLocation(latLng)
        }
    }

    private fun centerMapOnLocation(latLng: LatLng, zoom: Float = 15f) {
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, zoom)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
