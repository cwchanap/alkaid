package com.example.alkaid.ui.constellation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.alkaid.data.astronomy.Constellation
import com.example.alkaid.data.astronomy.Star
import com.example.alkaid.data.repository.BaseSensorRepository
import com.example.alkaid.data.repository.ConstellationRepository
import com.example.alkaid.data.sensor.LocationData
import com.example.alkaid.data.sensor.SensorResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class ConstellationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var application: Application
    private lateinit var constellationRepository: ConstellationRepository
    private lateinit var gpsRepository: BaseSensorRepository<LocationData>
    private lateinit var gpsFlow: MutableStateFlow<SensorResult>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        constellationRepository = mockk()
        gpsFlow = MutableStateFlow(SensorResult.Loading)
        gpsRepository = mockk {
            every { getSensorData() } returns gpsFlow
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads constellations from repository`() {
        val expected = listOf(
            Constellation(
                name = "Test",
                stars = listOf(Star("Alpha", 1.0, 2.0))
            )
        )
        every { constellationRepository.getConstellations() } returns expected

        val viewModel = ConstellationViewModel(
            application = application,
            constellationRepository = constellationRepository,
            gpsRepository = gpsRepository
        )

        assertEquals(expected, viewModel.constellations.value)
    }

    @Test
    fun `gps data updates location state`() {
        every { constellationRepository.getConstellations() } returns emptyList()
        val viewModel = ConstellationViewModel(
            application = application,
            constellationRepository = constellationRepository,
            gpsRepository = gpsRepository
        )
        val location = LocationData(35.0, 139.0, 50.0, 4.0f)

        gpsFlow.value = SensorResult.Data(location)

        assertEquals(location, viewModel.location.value)
    }

    @Test
    fun `gps loading does not overwrite last known location`() {
        every { constellationRepository.getConstellations() } returns emptyList()
        val viewModel = ConstellationViewModel(
            application = application,
            constellationRepository = constellationRepository,
            gpsRepository = gpsRepository
        )
        val location = LocationData(35.0, 139.0, 50.0, 4.0f)
        gpsFlow.value = SensorResult.Data(location)

        gpsFlow.value = SensorResult.Loading

        assertEquals(location, viewModel.location.value)
    }

    @Test
    fun `gps error does not overwrite last known location`() {
        every { constellationRepository.getConstellations() } returns emptyList()
        val viewModel = ConstellationViewModel(
            application = application,
            constellationRepository = constellationRepository,
            gpsRepository = gpsRepository
        )
        val location = LocationData(35.0, 139.0, 50.0, 4.0f)
        gpsFlow.value = SensorResult.Data(location)

        gpsFlow.value = SensorResult.Error("gps unavailable")

        assertEquals(location, viewModel.location.value)
    }

    @Test
    fun `view model exposes application only constructor for default factory`() {
        val constructor = ConstellationViewModel::class.java.getConstructor(Application::class.java)

        assertNotNull(constructor)
    }
}
