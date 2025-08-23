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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.text.TextWatcher
import android.text.Editable
import android.content.pm.PackageManager

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
    private var osmSearchMarker: Marker? = null
    private var useOsm: Boolean = false
    private var suggestionsAdapter: ArrayAdapter<String>? = null
    private var currentSuggestions: List<SearchItem> = emptyList()
    private var searchDebounceJob: Job? = null
    private var searchTextWatcher: TextWatcher? = null
    private var lastQueryLen: Int = 0
    private var lastQueryText: String = ""

    private val nominatimCache = object : LinkedHashMap<String, List<SearchItem>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<SearchItem>>?): Boolean = size > 50
    }

    // Default fallback location (San Francisco, CA)
    private val defaultLat = 37.7749
    private val defaultLon = -122.4194
    private var defaultZoom = 9.0
    private var defaultZoomF = 9f

    // Track whether we've already centered once per provider
    private var googleCenteredOnce = false
    private var osmCenteredOnce = false

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
        // Load default zoom from preferences
        defaultZoomF = mapPreferences.getDefaultZoom()
        defaultZoom = defaultZoomF.toDouble()

        // Only create Google map when selected; OSM is default via preferences
        setupObservers()
        setupFab()
        setupProviderToggle()
        setupOsmSearch()
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
                // Ensure gesture zooming and panning are enabled
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = true
                @Suppress("DEPRECATION")
                runCatching { uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = true }
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
            if (!googleCenteredOnce) {
                centerMapOnLocation(latLng, defaultZoomF)
                googleCenteredOnce = true
            } else {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            }
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
            if (!osmCenteredOnce) {
                centerOsmOnLocation(point, defaultZoom)
                osmCenteredOnce = true
            } else {
                osmMapView?.controller?.animateTo(point)
            }
            mapView.invalidate()
        }
    }

    private fun centerMapOnCurrentLocation() {
        viewModel.getCurrentLocation()?.let { locationData ->
            if (useOsm) {
                // Pan without changing zoom
                osmMapView?.controller?.animateTo(GeoPoint(locationData.latitude, locationData.longitude))
            } else {
                val latLng = LatLng(locationData.latitude, locationData.longitude)
                // Pan without changing zoom
                googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            }
        }
    }

    private fun centerMapOnLocation(latLng: LatLng, zoom: Float = 9f) {
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, zoom)
        )
    }

    private fun centerOsmOnLocation(point: GeoPoint, zoom: Double = 9.0) {
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
        // Hide OSM search bar
        binding.osmSearchContainer.visibility = View.GONE
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
        // Show OSM search bar
        binding.osmSearchContainer.visibility = View.VISIBLE
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
            centerMapOnLocation(latLng, defaultZoomF)
            googleCenteredOnce = true
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
            centerOsmOnLocation(point, defaultZoom)
            osmCenteredOnce = true
            mapView.invalidate()
        }
    }

    private fun initializeOsmMap() {
        // Configure osmdroid
        Configuration.getInstance().userAgentValue = requireContext().packageName
        osmMapView = binding.osmMap.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // Enable built-in zoom controls and pinch-zoom gestures
            setBuiltInZoomControls(true)
        }
    }

    private fun setupOsmSearch() {
        // Prepare adapter for suggestions
        suggestionsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        binding.osmSearchInput.setAdapter(suggestionsAdapter)

        binding.osmSearchInput.setOnItemClickListener { _: AdapterView<*>, _, idx, _ ->
            val item = currentSuggestions.getOrNull(idx) ?: return@setOnItemClickListener
            // Apply selection behavior
            val point = GeoPoint(item.lat, item.lon)
            // Remove previous search marker
            osmSearchMarker?.let { m -> osmMapView?.overlays?.remove(m) }
            osmSearchMarker = Marker(osmMapView).apply {
                this.position = point
                title = item.name
                snippet = "Lat: %.5f, Lng: %.5f".format(item.lat, item.lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            osmMapView?.overlays?.add(osmSearchMarker)
            osmMapView?.controller?.setZoom(14.0)
            osmMapView?.controller?.animateTo(point)
            osmMapView?.invalidate()
        }

        // Show dropdown on focus if we already have suggestions
        binding.osmSearchInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && suggestionsAdapter?.count ?: 0 > 0) {
                binding.osmSearchInput.showDropDown()
            }
        }

        // Debounced query on text changes, with immediate fetch when crossing threshold
        searchTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim().orEmpty()
                searchDebounceJob?.cancel()
                if (!useOsm) return
                if (q.length < 3) {
                    currentSuggestions = emptyList()
                    suggestionsAdapter?.clear()
                    binding.osmSearchInput.dismissDropDown()
                    lastQueryLen = q.length
                    lastQueryText = q
                    return
                }
                val key = q.lowercase(java.util.Locale.ROOT)
                val immediate = lastQueryLen < 3 && q.length >= 3
                lastQueryLen = q.length
                lastQueryText = q
                // If cache has data, show it right away for responsiveness
                nominatimCache[key]?.let { cached ->
                    currentSuggestions = cached
                    suggestionsAdapter?.apply {
                        clear()
                        addAll(cached.map { it.name })
                        notifyDataSetChanged()
                    }
                    if (binding.osmSearchInput.isFocused) {
                        // Post to ensure dropdown updates after dataset change
                        binding.osmSearchInput.post { binding.osmSearchInput.showDropDown() }
                    }
                }
                if (immediate) {
                    // Fetch immediately when user first reaches threshold
                    fetchAndShowSuggestions(q)
                } else {
                    searchDebounceJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(250)
                        // Only fetch if text hasn't changed again
                        if (binding.osmSearchInput.text?.toString()?.trim() == q) {
                            fetchAndShowSuggestions(q)
                        }
                    }
                }
            }
        }
        binding.osmSearchInput.addTextChangedListener(searchTextWatcher)

        // End icon click triggers search
        binding.osmSearchContainer.setEndIconOnClickListener {
            val q = binding.osmSearchInput.text?.toString()?.trim().orEmpty()
            if (q.isNotEmpty()) performOsmSearch(q)
        }
        // Keyboard action Search/Done
        binding.osmSearchInput.setOnEditorActionListener { v, _, _ ->
            val text = v.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                performOsmSearch(text)
                true
            } else false
        }
    }

    private fun fetchAndShowSuggestions(q: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val key = q.lowercase(java.util.Locale.ROOT)
            val items: List<SearchItem>? = nominatimCache[key] ?: withContext(Dispatchers.IO) {
                runCatching { nominatimSearch(q) }.onSuccess { nominatimCache[key] = it }.getOrNull()
            }
            if (items == null) return@launch
            currentSuggestions = items
            suggestionsAdapter?.apply {
                clear()
                addAll(items.map { it.name })
                notifyDataSetChanged()
            }
            if (binding.osmSearchInput.isFocused) {
                // Post to ensure UI is ready to render updated dropdown
                binding.osmSearchInput.post { binding.osmSearchInput.showDropDown() }
            }
        }
    }

    private fun performOsmSearch(query: String) {
        // Disable input while searching
        binding.osmSearchContainer.isEnabled = false
        val originalHint = binding.osmSearchInput.hint
        binding.osmSearchInput.hint = getString(R.string.sensor_loading)

        viewLifecycleOwner.lifecycleScope.launch {
            val items: List<SearchItem>? = withContext(Dispatchers.IO) {
                runCatching { nominatimSearch(query) }.getOrNull()
            }
            // Restore UI state
            binding.osmSearchContainer.isEnabled = true
            binding.osmSearchInput.hint = originalHint

            if (items == null) {
                android.widget.Toast.makeText(requireContext(), getString(R.string.map_search_error), android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (items.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), getString(R.string.map_search_no_results), android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }

            val top = items.first()
            val point = GeoPoint(top.lat, top.lon)
            // Remove previous search marker
            osmSearchMarker?.let { m -> osmMapView?.overlays?.remove(m) }
            osmSearchMarker = Marker(osmMapView).apply {
                position = point
                title = top.name
                snippet = "Lat: %.5f, Lng: %.5f".format(top.lat, top.lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            osmMapView?.overlays?.add(osmSearchMarker)
            // Center and zoom to searched point
            osmMapView?.controller?.setZoom(14.0)
            osmMapView?.controller?.animateTo(point)
            osmMapView?.invalidate()
        }
    }

    private data class SearchItem(val name: String, val lat: Double, val lon: Double)

    private fun nominatimSearch(query: String): List<SearchItem> {
        val encoded = java.net.URLEncoder.encode(query, Charsets.UTF_8.name())
        val url = java.net.URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=5&addressdetails=0")
        val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            // Nominatim usage policy recommends a descriptive UA
            setRequestProperty("User-Agent", "${requireContext().packageName}/${getAppVersionName()}")
            setRequestProperty("Accept-Language", java.util.Locale.getDefault().toLanguageTag())
        }
        conn.inputStream.use { input ->
            val body = input.bufferedReader().readText()
            val arr = org.json.JSONArray(body)
            val items = mutableListOf<SearchItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("display_name", "")
                val lat = obj.optString("lat", null)?.toDoubleOrNull() ?: continue
                val lon = obj.optString("lon", null)?.toDoubleOrNull() ?: continue
                items.add(SearchItem(name = name, lat = lat, lon = lon))
            }
            return items
        }
    }

    private fun getAppVersionName(): String {
        return try {
            val pkg = requireContext().packageName
            val pm = requireContext().packageManager
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)).versionName ?: ""
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0).versionName ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up watchers and jobs
        searchDebounceJob?.cancel()
        searchDebounceJob = null
        searchTextWatcher?.let { watcher -> binding.osmSearchInput.removeTextChangedListener(watcher) }
        searchTextWatcher = null
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        osmMapView?.onResume()
        // Refresh default zoom from preferences in case it changed in Settings
        defaultZoomF = mapPreferences.getDefaultZoom()
        defaultZoom = defaultZoomF.toDouble()
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
