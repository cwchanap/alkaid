# Phase 1: Foundations Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Refactor HomeFragment to proper MVVM and add dedicated sensor icons across the app.

**Architecture:** Move all repository management, sensor observation, and preference logic from HomeFragment into HomeViewModel. HomeViewModel exposes a single `StateFlow` of UI state. HomeFragment becomes a thin view layer. Separately, replace all generic placeholder icons with sensor-specific Material vector drawables.

**Tech Stack:** Kotlin, AndroidViewModel, StateFlow, MockK, JUnit, Robolectric, Material vector drawables

---

## Task 1: Create HomeUiState Data Model

**Files:**
- Create: `app/src/main/java/com/example/alkaid/ui/home/HomeUiState.kt`

**Step 1: Create the UI state file**

```kotlin
package com.example.alkaid.ui.home

import com.example.alkaid.data.sensor.SensorResult
import com.example.alkaid.data.sensor.SensorType

data class HomeUiState(
    val gpsResult: SensorResult = SensorResult.Loading,
    val visibleSensors: List<SensorType> = emptyList()
)
```

This is the single state object HomeViewModel will expose. `gpsResult` drives the GPS widget. `visibleSensors` drives the RecyclerView grid (GPS filtered out).

**Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/alkaid/ui/home/HomeUiState.kt
git commit -m "feat: add HomeUiState data model for MVVM refactor"
```

---

## Task 2: Rewrite HomeViewModel

**Files:**
- Modify: `app/src/main/java/com/example/alkaid/ui/home/HomeViewModel.kt`

**Step 1: Replace HomeViewModel with proper implementation**

Replace the entire file contents with:

```kotlin
package com.example.alkaid.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alkaid.data.preferences.SensorVisibilityPreferences
import com.example.alkaid.data.repository.AccelerometerRepository
import com.example.alkaid.data.repository.BarometerRepository
import com.example.alkaid.data.repository.BaseSensorRepository
import com.example.alkaid.data.repository.GpsRepository
import com.example.alkaid.data.repository.GyroscopeRepository
import com.example.alkaid.data.repository.HumiditySensorRepository
import com.example.alkaid.data.repository.LightSensorRepository
import com.example.alkaid.data.repository.MagnetometerRepository
import com.example.alkaid.data.repository.TemperatureRepository
import com.example.alkaid.data.sensor.SensorResult
import com.example.alkaid.data.sensor.SensorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    internal var sensorRepositories: Map<SensorType, BaseSensorRepository<*>> = mapOf(
        SensorType.BAROMETER to BarometerRepository(application.applicationContext),
        SensorType.GYROSCOPE to GyroscopeRepository(application.applicationContext),
        SensorType.TEMPERATURE to TemperatureRepository(application.applicationContext),
        SensorType.GPS to GpsRepository(application.applicationContext),
        SensorType.ACCELEROMETER to AccelerometerRepository(application.applicationContext),
        SensorType.MAGNETOMETER to MagnetometerRepository(application.applicationContext),
        SensorType.LIGHT to LightSensorRepository(application.applicationContext),
        SensorType.HUMIDITY to HumiditySensorRepository(application.applicationContext),
    )

    internal var sensorPreferences = SensorVisibilityPreferences(application.applicationContext)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var gpsObservationStarted = false

    fun startObserving() {
        observeVisibleSensors()
        observeGps()
    }

    private fun observeVisibleSensors() {
        sensorPreferences.getVisibleSensorsFlow()
            .onEach { visibleSensors ->
                _uiState.value = _uiState.value.copy(
                    visibleSensors = visibleSensors.filter { it != SensorType.GPS }
                )
            }
            .launchIn(viewModelScope)
    }

    private fun observeGps() {
        if (gpsObservationStarted) return
        gpsObservationStarted = true

        val gpsRepo = sensorRepositories[SensorType.GPS] ?: return
        gpsRepo.getSensorData()
            .onEach { result ->
                _uiState.value = _uiState.value.copy(gpsResult = result)
            }
            .launchIn(viewModelScope)
    }
}
```

Key decisions:
- Extends `AndroidViewModel` (same as `MapViewModel` and `WeatherViewModel`) to access application context
- `sensorRepositories` and `sensorPreferences` are `internal var` so tests can swap them
- `startObserving()` is called from the Fragment's `onViewCreated` to avoid touching sensors during construction (same pattern as `MapViewModel.onMapReady()`)
- GPS is observed separately since it feeds the dedicated widget

**Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/alkaid/ui/home/HomeViewModel.kt
git commit -m "feat: rewrite HomeViewModel with repository management and state flow"
```

---

## Task 3: Write HomeViewModel Tests

**Files:**
- Modify: `app/src/test/java/com/example/alkaid/ui/home/HomeViewModelTest.kt`

**Step 1: Replace the test file with real tests**

```kotlin
package com.example.alkaid.ui.home

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.alkaid.data.preferences.SensorVisibilityPreferences
import com.example.alkaid.data.repository.BaseSensorRepository
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.data.sensor.SensorResult
import com.example.alkaid.data.sensor.SensorType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var application: Application
    private lateinit var viewModel: HomeViewModel
    private lateinit var fakeGpsRepo: BaseSensorRepository<LocationData>
    private lateinit var gpsFlow: MutableStateFlow<SensorResult>
    private lateinit var sensorPreferences: SensorVisibilityPreferences

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()

        // Create a fake GPS repository
        gpsFlow = MutableStateFlow<SensorResult>(SensorResult.Loading)
        fakeGpsRepo = mockk {
            every { getSensorData() } returns gpsFlow
        }

        // Create real preferences backed by a mock SharedPreferences
        sensorPreferences = SensorVisibilityPreferences(application)

        viewModel = HomeViewModel(application)
        viewModel.sensorRepositories = mapOf(SensorType.GPS to fakeGpsRepo)
        viewModel.sensorPreferences = sensorPreferences
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading with empty sensor list`() {
        val state = viewModel.uiState.value
        assertEquals(SensorResult.Loading, state.gpsResult)
        assertTrue(state.visibleSensors.isEmpty())
    }

    @Test
    fun `gps data updates uiState gpsResult`() = runTest {
        viewModel.startObserving()

        val locationData = LocationData(37.7749, -122.4194, 100.0, 10.0f)
        gpsFlow.value = SensorResult.Data(locationData)

        val state = viewModel.uiState.value
        assertTrue(state.gpsResult is SensorResult.Data<*>)
        val data = (state.gpsResult as SensorResult.Data<*>).value as LocationData
        assertEquals(37.7749, data.latitude, 0.0)
    }

    @Test
    fun `gps error updates uiState gpsResult`() = runTest {
        viewModel.startObserving()

        gpsFlow.value = SensorResult.Error("No permission")

        val state = viewModel.uiState.value
        assertTrue(state.gpsResult is SensorResult.Error)
        assertEquals("No permission", (state.gpsResult as SensorResult.Error).message)
    }

    @Test
    fun `visible sensors flow filters out GPS`() = runTest {
        // Enable only GPS and barometer
        sensorPreferences.resetToDefaults()

        viewModel.startObserving()

        val state = viewModel.uiState.value
        // GPS should be filtered out from the grid list
        assertTrue(SensorType.GPS !in state.visibleSensors)
    }
}
```

**Step 2: Run the tests**

Run: `./gradlew testDebugUnitTest --tests "com.example.alkaid.ui.home.HomeViewModelTest"`
Expected: All 4 tests PASS

**Step 3: Commit**

```bash
git add app/src/test/java/com/example/alkaid/ui/home/HomeViewModelTest.kt
git commit -m "test: add HomeViewModel unit tests for GPS and sensor visibility"
```

---

## Task 4: Refactor HomeFragment to Use HomeViewModel

**Files:**
- Modify: `app/src/main/java/com/example/alkaid/ui/home/HomeFragment.kt`

**Step 1: Rewrite HomeFragment as a thin view layer**

```kotlin
package com.example.alkaid.ui.home

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.alkaid.data.sensor.SensorType
import com.example.alkaid.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var sensorAdapter: SensorAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeUiState()
        viewModel.startObserving()
    }

    private fun setupRecyclerView() {
        val spanCount = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> 3
            else -> 2
        }

        binding.sensorRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        sensorAdapter = SensorAdapter(viewModel.sensorRepositories, viewLifecycleOwner)
        binding.sensorRecyclerView.adapter = sensorAdapter
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update GPS widget
                    binding.gpsLocationWidget.updateLocation(state.gpsResult)

                    // Update sensor grid
                    if (state.visibleSensors.isEmpty()) {
                        binding.sensorRecyclerView.visibility = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                    } else {
                        binding.sensorRecyclerView.visibility = View.VISIBLE
                        binding.emptyState.visibility = View.GONE
                        sensorAdapter.submitList(state.visibleSensors)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

Key changes:
- Removed all repository imports and construction
- Removed `sensorPreferences` — now lives in ViewModel
- Uses `by viewModels()` delegate
- Single `collect` on `uiState` drives both the GPS widget and sensor grid
- Uses `repeatOnLifecycle(STARTED)` for proper lifecycle handling

**Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Run all tests**

Run: `./gradlew testDebugUnitTest`
Expected: All tests PASS

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/alkaid/ui/home/HomeFragment.kt
git commit -m "refactor: HomeFragment now delegates to HomeViewModel for all state"
```

---

## Task 5: Add Sensor-Specific Vector Drawable Icons

**Files:**
- Create: `app/src/main/res/drawable/ic_sensor_barometer.xml`
- Create: `app/src/main/res/drawable/ic_sensor_gyroscope.xml`
- Create: `app/src/main/res/drawable/ic_sensor_temperature.xml`
- Create: `app/src/main/res/drawable/ic_sensor_gps.xml`
- Create: `app/src/main/res/drawable/ic_sensor_accelerometer.xml`
- Create: `app/src/main/res/drawable/ic_sensor_magnetometer.xml`
- Create: `app/src/main/res/drawable/ic_sensor_light.xml`
- Create: `app/src/main/res/drawable/ic_sensor_humidity.xml`

**Step 1: Create all 8 icon files**

`ic_sensor_barometer.xml` — Speed/gauge icon (Material "speed"):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M20.38,8.57l-1.23,1.85a8,8 0,0 1,-0.22 7.58L5.07,18A8,8 0,0 1,15.58 6.85l1.85,-1.23A10,10 0,0 0,3.35 19a2,2 0,0 0,1.72 1h13.85a2,2 0,0 0,1.74 -1,10 10,0 0,0 -0.27,-10.44zM10.59,15.41a2,2 0,0 0,2.83 0l5.66,-8.49 -8.49,5.66a2,2 0,0 0,0 2.83z" />
</vector>
```

`ic_sensor_gyroscope.xml` — Rotate/3D rotation icon (Material "3d_rotation"):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M7.52,21.48C4.25,19.94 1.91,16.76 1.55,13L0.05,13C0.56,19.16 5.71,24 12,24l0.66,-0.03 -3.81,-3.81 -1.33,1.32zM16.48,2.52C19.75,4.06 22.09,7.24 22.45,11L23.95,11C23.44,4.84 18.29,0 12,0l-0.66,0.03 3.81,3.81 1.33,-1.32z" />
    <path
        android:fillColor="@android:color/white"
        android:pathData="M16,9.01l-4.41,0c-0.78,0 -1.53,0.36 -2.05,0.97 -0.5,0.58 -0.74,1.35 -0.62,2.11l0.12,0.78c0.21,1.36 1.46,2.14 2.87,2.14h1.22l-0.02,0.98c0,0.45 -0.19,0.88 -0.53,1.2 -0.34,0.31 -0.79,0.48 -1.27,0.48h-2.14l0,1.33h2.14c0.84,0 1.64,-0.32 2.24,-0.89 0.59,-0.57 0.93,-1.33 0.93,-2.13v-0.98h0.47c0.56,0 1.04,-0.44 1.04,-0.94L16,10.04c0.01,-0.56 -0.46,-1.03 -1,-1.03zM14.67,13.67h-2.77c-0.58,0 -1.12,-0.28 -1.2,-0.78l-0.12,-0.78c-0.04,-0.26 0.04,-0.52 0.22,-0.72 0.17,-0.2 0.43,-0.32 0.69,-0.32h3.17v2.6z" />
</vector>
```

`ic_sensor_temperature.xml` — Thermostat icon (Material "thermostat"):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M15,13L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v8c-1.21,0.91 -2,2.37 -2,4 0,2.76 2.24,5 5,5s5,-2.24 5,-5c0,-1.63 -0.79,-3.09 -2,-4zM12,4c0.55,0 1,0.45 1,1v1h-2L11,5c0,-0.55 0.45,-1 1,-1z" />
</vector>
```

`ic_sensor_gps.xml` — My Location icon (Material "my_location"):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,8c-2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4 -1.79,-4 -4,-4zM20.94,11c-0.46,-4.17 -3.77,-7.48 -7.94,-7.94L13,1h-2v2.06C6.83,3.52 3.52,6.83 3.06,11L1,11v2h2.06c0.46,4.17 3.77,7.48 7.94,7.94L11,23h2v-2.06c4.17,-0.46 7.48,-3.77 7.94,-7.94L23,13v-2h-2.06zM12,19c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z" />
</vector>
```

`ic_sensor_accelerometer.xml` — Vibration icon (Material "vibration"):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M0,15h2L2,9L0,9v6zM3,17h2L5,7L3,7v10zM22,9v6h2L24,9h-2zM19,17h2L21,7h-2v10zM16.5,3h-9C6.67,3 6,3.67 6,4.5v15c0,0.83 0.67,1.5 1.5,1.5h9c0.83,0 1.5,-0.67 1.5,-1.5v-15c0,-0.83 -0.67,-1.5 -1.5,-1.5zM16,19L8,19L8,5h8v14z" />
</vector>
```

`ic_sensor_magnetometer.xml` — Explore/compass icon (Material "explore"):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8zM6.5,17.5l7.51,-3.49L17.5,6.5 9.99,9.99 6.5,17.5zM12,10.9c0.61,0 1.1,0.49 1.1,1.1s-0.49,1.1 -1.1,1.1 -1.1,-0.49 -1.1,-1.1 0.49,-1.1 1.1,-1.1z" />
</vector>
```

`ic_sensor_light.xml` — Light bulb icon (Material "light_mode"):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5 5,-2.24 5,-5 -2.24,-5 -5,-5zM2,13h2c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1L2,11c-0.55,0 -1,0.45 -1,1s0.45,1 1,1zM20,13h2c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1h-2c-0.55,0 -1,0.45 -1,1s0.45,1 1,1zM11,2v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1L13,2c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1zM11,20v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1v-2c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1zM5.99,4.58c-0.39,-0.39 -1.03,-0.39 -1.41,0 -0.39,0.39 -0.39,1.03 0,1.41l1.06,1.06c0.39,0.39 1.03,0.39 1.41,0s0.39,-1.03 0,-1.41L5.99,4.58zM18.36,16.95c-0.39,-0.39 -1.03,-0.39 -1.41,0 -0.39,0.39 -0.39,1.03 0,1.41l1.06,1.06c0.39,0.39 1.03,0.39 1.41,0 0.39,-0.39 0.39,-1.03 0,-1.41l-1.06,-1.06zM19.42,5.99c0.39,-0.39 0.39,-1.03 0,-1.41 -0.39,-0.39 -1.03,-0.39 -1.41,0l-1.06,1.06c-0.39,0.39 -0.39,1.03 0,1.41s1.03,0.39 1.41,0l1.06,-1.06zM7.05,18.36c0.39,-0.39 0.39,-1.03 0,-1.41 -0.39,-0.39 -1.03,-0.39 -1.41,0l-1.06,1.06c-0.39,0.39 -0.39,1.03 0,1.41s1.03,0.39 1.41,0l1.06,-1.06z" />
</vector>
```

`ic_sensor_humidity.xml` — Water drop icon (Material "water_drop"):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,2c-5.33,4.55 -8,8.48 -8,11.8 0,4.98 3.8,8.2 8,8.2s8,-3.22 8,-8.2c0,-3.32 -2.67,-7.25 -8,-11.8zM12,20c-3.35,0 -6,-2.57 -6,-6.2 0,-2.34 1.95,-5.44 6,-9.14 4.05,3.7 6,6.79 6,9.14 0,3.63 -2.65,6.2 -6,6.2z" />
</vector>
```

**Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/res/drawable/ic_sensor_*.xml
git commit -m "feat: add dedicated vector drawable icons for all 8 sensor types"
```

---

## Task 6: Update SensorCardView Icon Mapping

**Files:**
- Modify: `app/src/main/java/com/example/alkaid/ui/components/SensorCardView.kt` (lines 53-63)

**Step 1: Replace the icon `when` block**

In `SensorCardView.setupSensor()`, replace:

```kotlin
        val iconRes = when (sensorType) {
            SensorType.BAROMETER -> R.drawable.ic_home_black_24dp
            SensorType.GYROSCOPE -> R.drawable.ic_settings_black_24dp
            SensorType.TEMPERATURE -> R.drawable.ic_home_black_24dp
            SensorType.GPS -> R.drawable.ic_map_black_24dp
            SensorType.ACCELEROMETER -> R.drawable.ic_home_black_24dp
            SensorType.MAGNETOMETER -> R.drawable.ic_map_black_24dp
            SensorType.LIGHT -> R.drawable.ic_home_black_24dp
            SensorType.HUMIDITY -> R.drawable.ic_home_black_24dp
        }
```

With:

```kotlin
        val iconRes = when (sensorType) {
            SensorType.BAROMETER -> R.drawable.ic_sensor_barometer
            SensorType.GYROSCOPE -> R.drawable.ic_sensor_gyroscope
            SensorType.TEMPERATURE -> R.drawable.ic_sensor_temperature
            SensorType.GPS -> R.drawable.ic_sensor_gps
            SensorType.ACCELEROMETER -> R.drawable.ic_sensor_accelerometer
            SensorType.MAGNETOMETER -> R.drawable.ic_sensor_magnetometer
            SensorType.LIGHT -> R.drawable.ic_sensor_light
            SensorType.HUMIDITY -> R.drawable.ic_sensor_humidity
        }
```

**Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/alkaid/ui/components/SensorCardView.kt
git commit -m "feat: use dedicated sensor icons in SensorCardView"
```

---

## Task 7: Update Settings Layout Icons

**Files:**
- Modify: `app/src/main/res/layout/fragment_settings.xml`

**Step 1: Replace all generic icons in the settings layout**

Apply these replacements in `fragment_settings.xml`:

| Line | Old `android:src` | New `android:src` |
|------|-------------------|-------------------|
| 44 | `@drawable/ic_home_black_24dp` | `@drawable/ic_sensor_barometer` |
| 124 | `@drawable/ic_settings_black_24dp` | `@drawable/ic_sensor_gyroscope` |
| 162 | `@drawable/ic_home_black_24dp` | `@drawable/ic_sensor_temperature` |
| 200 | `@drawable/ic_map_black_24dp` | `@drawable/ic_sensor_gps` |
| 238 | `@drawable/ic_home_black_24dp` | `@drawable/ic_sensor_accelerometer` |
| 276 | `@drawable/ic_map_black_24dp` | `@drawable/ic_sensor_magnetometer` |
| 314 | `@drawable/ic_home_black_24dp` | `@drawable/ic_sensor_light` |
| 352 | `@drawable/ic_settings_black_24dp` | `@drawable/ic_sensor_humidity` |

**Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/res/layout/fragment_settings.xml
git commit -m "feat: use dedicated sensor icons in settings layout"
```

---

## Task 8: Update GPS Widget Icon

**Files:**
- Modify: `app/src/main/res/layout/gps_location_widget.xml` (line 29)

**Step 1: Replace the GPS widget icon**

In `gps_location_widget.xml`, change line 29:

From: `android:src="@drawable/ic_map_black_24dp"`
To: `android:src="@drawable/ic_sensor_gps"`

**Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/res/layout/gps_location_widget.xml
git commit -m "feat: use GPS-specific icon in location widget"
```

---

## Task 9: Run Full Test Suite and Lint

**Step 1: Run all unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: All tests PASS

**Step 2: Run lint**

Run: `./gradlew lint`
Expected: No new errors (existing baseline issues are acceptable)

**Step 3: Build debug APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Summary

| Task | What | Files Changed |
|------|------|---------------|
| 1 | HomeUiState data model | 1 created |
| 2 | Rewrite HomeViewModel | 1 modified |
| 3 | HomeViewModel tests | 1 modified |
| 4 | Refactor HomeFragment | 1 modified |
| 5 | Sensor icon drawables | 8 created |
| 6 | SensorCardView icon mapping | 1 modified |
| 7 | Settings layout icons | 1 modified |
| 8 | GPS widget icon | 1 modified |
| 9 | Full verification | 0 |

**Total: 9 files created, 4 files modified, 0 deleted**
