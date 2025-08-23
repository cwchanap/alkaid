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

// osmdroid (OpenStreetMap) imports
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.example.alkaid.data.preferences.MapPreferences
import com.example.alkaid.data.preferences.MapPreferences.MapProvider

/**
 * Fragment that displays a Google Map with current location tracking
 */
class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MapViewModel
    private lateinit var mapPreferences: MapPreferences
    private var googleMap: GoogleMap? = null
    private var googleMapFragment: SupportMapFragment? = null
    private var currentLocationMarker: com.google.android.gms.maps.model.Marker? = null

    // OSM state
    private var osmMapView: MapView? = null
    private var osmMarker: Marker? = null
    private var useOsm: Boolean = false

    // Default fallback location (San Francisco, CA)
    private val defaultLat = 37.7749
    private val defaultLon = -122.4194

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

        mapPreferences = MapPreferences(requireContext())

        // Only create Google map when selected; OSM is default via preferences
        setupObservers()
        setupFab()
        setupProviderToggle()
    }

    private fun setupMap() { /* no-op; created on demand */ }

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
        } ?: run { ensureDefaultLocation() }
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
        if (useOsm) {
            updateOsmMapLocation(locationData)
        } else {
            updateGoogleMapLocation(locationData)
        }
    }

    private fun updateGoogleMapLocation(locationData: LocationData) {
        googleMap?.let { map ->
            val latLng = LatLng(locationData.latitude, locationData.longitude)
            currentLocationMarker?.remove()
            currentLocationMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.map_my_location))
                    .snippet("${locationData.getFormattedLatLng()}\nAltitude: ${locationData.getFormattedAltitude()}")
            )
            centerMapOnLocation(latLng)
        }
    }

    private fun updateOsmMapLocation(locationData: LocationData) {
        osmMapView?.let { mapView ->
            val point = GeoPoint(locationData.latitude, locationData.longitude)
            // Remove existing marker
            osmMarker?.let { marker -> mapView.overlays.remove(marker) }
            // Add new marker
            osmMarker = Marker(mapView).apply {
                position = point
                title = getString(R.string.map_my_location)
                snippet = "${locationData.getFormattedLatLng()}\nAltitude: ${locationData.getFormattedAltitude()}"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(osmMarker)
            centerOsmOnLocation(point)
            mapView.invalidate()
        }
    }

    private fun centerMapOnCurrentLocation() {
        viewModel.getCurrentLocation()?.let { locationData ->
            if (useOsm) {
                centerOsmOnLocation(GeoPoint(locationData.latitude, locationData.longitude))
            } else {
                val latLng = LatLng(locationData.latitude, locationData.longitude)
                centerMapOnLocation(latLng)
            }
        }
    }

    private fun centerMapOnLocation(latLng: LatLng, zoom: Float = 15f) {
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, zoom)
        )
    }

    private fun centerOsmOnLocation(point: GeoPoint, zoom: Double = 15.0) {
        osmMapView?.controller?.setZoom(zoom)
        osmMapView?.controller?.animateTo(point)
    }

    private fun setupProviderToggle() {
        binding.toggleMapProvider.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                binding.btnProviderGoogle.id -> {
                    mapPreferences.setProvider(MapProvider.GOOGLE)
                    showGoogleMap()
                }
                binding.btnProviderOsm.id -> {
                    mapPreferences.setProvider(MapProvider.OSM)
                    showOsmMap()
                }
            }
        }

        // Apply saved preference (default OSM)
        when (mapPreferences.getProvider()) {
            MapProvider.GOOGLE -> binding.toggleMapProvider.check(binding.btnProviderGoogle.id)
            MapProvider.OSM -> binding.toggleMapProvider.check(binding.btnProviderOsm.id)
        }
    }

    private fun showGoogleMap() {
        useOsm = false
        // Show Google by hiding the OSM overlay
        binding.osmMap.visibility = View.GONE
        // Lazily add Google Map fragment
        if (googleMapFragment == null) {
            googleMapFragment = SupportMapFragment.newInstance().also { fragment ->
                childFragmentManager
                    .beginTransaction()
                    .replace(R.id.google_map_container, fragment, "google_map")
                    .commitNowAllowingStateLoss()
                fragment.getMapAsync(this)
            }
        }
        // Update location on Google map if available
        viewModel.getCurrentLocation()?.let { updateGoogleMapLocation(it) } ?: run { showDefaultOnGoogleMap() }
    }

    private fun showOsmMap() {
        useOsm = true
        // Initialize osmdroid MapView lazily
        if (osmMapView == null) {
            initializeOsmMap()
        }
        // Show OSM overlay above the Google fragment
        binding.osmMap.visibility = View.VISIBLE
        // Update location on OSM map if available
        viewModel.getCurrentLocation()?.let { updateOsmMapLocation(it) } ?: run { showDefaultOnOsmMap() }
        if (!viewModel.isMapReady.value) {
            // Start location updates when OSM gets initialized and becomes visible
            viewModel.onMapReady()
        }
    }

    private fun ensureDefaultLocation() {
        if (useOsm) {
            showDefaultOnOsmMap()
        } else {
            showDefaultOnGoogleMap()
        }
    }

    private fun showDefaultOnGoogleMap() {
        googleMap?.let { map ->
            val latLng = LatLng(defaultLat, defaultLon)
            currentLocationMarker?.remove()
            currentLocationMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.map_default_location))
                    .snippet("Lat: %.4f, Lng: %.4f".format(defaultLat, defaultLon))
            )
            centerMapOnLocation(latLng)
        }
    }

    private fun showDefaultOnOsmMap() {
        osmMapView?.let { mapView ->
            val point = GeoPoint(defaultLat, defaultLon)
            osmMarker?.let { marker -> mapView.overlays.remove(marker) }
            osmMarker = Marker(mapView).apply {
                position = point
                title = getString(R.string.map_default_location)
                snippet = "Lat: %.4f, Lng: %.4f".format(defaultLat, defaultLon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(osmMarker)
            centerOsmOnLocation(point)
            mapView.invalidate()
        }
    }

    private fun initializeOsmMap() {
        // Configure osmdroid
        Configuration.getInstance().userAgentValue = requireContext().packageName
        osmMapView = binding.osmMap.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // Built-in zoom controls off; use gestures and FAB
            setBuiltInZoomControls(false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        osmMapView?.onResume()
        // Keep in-map toggle in sync with Settings
        val preferred = mapPreferences.getProvider()
        val targetId = if (preferred == MapProvider.OSM) binding.btnProviderOsm.id else binding.btnProviderGoogle.id
        if (binding.toggleMapProvider.checkedButtonId != targetId) {
            binding.toggleMapProvider.check(targetId)
        }
    }

    override fun onPause() {
        super.onPause()
        osmMapView?.onPause()
    }
}
