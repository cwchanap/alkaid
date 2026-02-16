package com.example.alkaid.ui.home

import android.app.Application
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

        gpsFlow = MutableStateFlow<SensorResult>(SensorResult.Loading)
        fakeGpsRepo = mockk {
            every { getSensorData() } returns gpsFlow
        }

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
        SensorType.entries.forEach { sensorPreferences.setSensorVisible(it, false) }
        sensorPreferences.setSensorVisible(SensorType.GPS, true)
        sensorPreferences.setSensorVisible(SensorType.BAROMETER, true)

        viewModel.startObserving()

        val state = viewModel.uiState.value
        // GPS should be filtered out from the grid list (only barometer should be visible)
        assertTrue(SensorType.GPS !in state.visibleSensors)
        assertTrue(SensorType.BAROMETER in state.visibleSensors)
    }
}
