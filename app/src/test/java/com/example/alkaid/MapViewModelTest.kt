package com.example.alkaid

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.alkaid.data.repository.FakeGpsRepository
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.data.sensor.SensorResult
import com.example.alkaid.ui.map.LocationState
import com.example.alkaid.ui.map.MapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class MapViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: MapViewModel
    private lateinit var fakeGpsRepository: FakeGpsRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val application = ApplicationProvider.getApplicationContext<Application>()
        fakeGpsRepository = FakeGpsRepository(application)
        viewModel = MapViewModel(application)
        viewModel.gpsRepository = fakeGpsRepository
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `MapViewModel is initialized correctly`() {
        assertNotNull(viewModel)
    }

    @Test
    fun `getCurrentLocation returns location data when state is Success`() {
        val locationData = LocationData(37.7749, -122.4194, 10.0, 5.0f)
        val successState = LocationState.Success(locationData)

        viewModel.setLocationStateForTesting(successState)

        val result = viewModel.getCurrentLocation()

        assertEquals(locationData, result)
    }

    @Test
    fun `getCurrentLocation returns null when state is not Success`() {
        viewModel.setLocationStateForTesting(LocationState.Loading)

        val result = viewModel.getCurrentLocation()

        assertNull(result)
    }

    @Test
    fun `onMapReady sets map ready and updates successful location state`() {
        val locationData = LocationData(22.3193, 114.1694, 15.0, 3.0f)
        fakeGpsRepository.setSensorData(SensorResult.Data(locationData))

        viewModel.onMapReady()

        assertEquals(true, viewModel.isMapReady.value)
        assertEquals(LocationState.Success(locationData), viewModel.locationState.value)
    }

    @Test
    fun `onMapReady updates error state when gps repository emits error`() {
        fakeGpsRepository.setSensorData(SensorResult.Error("GPS unavailable"))

        viewModel.onMapReady()

        assertEquals(LocationState.Error("GPS unavailable"), viewModel.locationState.value)
    }
}
