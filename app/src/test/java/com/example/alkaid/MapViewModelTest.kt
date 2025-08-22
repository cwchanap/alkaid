package com.example.alkaid

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.alkaid.data.repository.FakeGpsRepository
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.ui.map.LocationState
import com.example.alkaid.ui.map.MapViewModel
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private lateinit var viewModel: MapViewModel
    private lateinit var fakeGpsRepository: FakeGpsRepository

    @Before
    fun setup() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        fakeGpsRepository = FakeGpsRepository(application)
        viewModel = MapViewModel(application)
        viewModel.gpsRepository = fakeGpsRepository
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
}
