package com.example.alkaid.ui.home

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
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
import com.example.alkaid.data.sensor.SensorType
import com.example.alkaid.databinding.FragmentHomeBinding
import com.example.alkaid.ui.components.GpsLocationWidget
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorAdapter: SensorAdapter
    private lateinit var sensorPreferences: SensorVisibilityPreferences
    
    // Sensor repositories
    private val sensorRepositories = mutableMapOf<SensorType, BaseSensorRepository<*>>()

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
        
        setupSensorRepositories()
        setupRecyclerView()
        setupPreferences()
        setupGpsWidget()
        observeVisibleSensors()
    }

    private fun setupSensorRepositories() {
        sensorRepositories[SensorType.BAROMETER] = BarometerRepository(requireContext())
        sensorRepositories[SensorType.GYROSCOPE] = GyroscopeRepository(requireContext())
        sensorRepositories[SensorType.TEMPERATURE] = TemperatureRepository(requireContext())
        sensorRepositories[SensorType.GPS] = GpsRepository(requireContext())
        sensorRepositories[SensorType.ACCELEROMETER] = AccelerometerRepository(requireContext())
        sensorRepositories[SensorType.MAGNETOMETER] = MagnetometerRepository(requireContext())
        sensorRepositories[SensorType.LIGHT] = LightSensorRepository(requireContext())
        sensorRepositories[SensorType.HUMIDITY] = HumiditySensorRepository(requireContext())
    }

    private fun setupRecyclerView() {
        // Calculate span count based on orientation and screen size
        val spanCount = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> 3
            else -> 2
        }
        
        val layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.sensorRecyclerView.layoutManager = layoutManager
        
        sensorAdapter = SensorAdapter(sensorRepositories, this)
        binding.sensorRecyclerView.adapter = sensorAdapter
    }

    private fun setupPreferences() {
        sensorPreferences = SensorVisibilityPreferences(requireContext())
    }

    private fun setupGpsWidget() {
        // Observe GPS data and update the widget
        val gpsRepository = sensorRepositories[SensorType.GPS] as? GpsRepository
        gpsRepository?.getSensorData()
            ?.onEach { sensorResult ->
                binding.gpsLocationWidget.updateLocation(sensorResult)
            }
            ?.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun observeVisibleSensors() {
        sensorPreferences.getVisibleSensorsFlow()
            .onEach { visibleSensors ->
                updateSensorList(visibleSensors)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun updateSensorList(visibleSensors: List<SensorType>) {
        // Filter out GPS since it has its own dedicated widget
        val sensorsForGrid = visibleSensors.filter { it != SensorType.GPS }
        
        if (sensorsForGrid.isEmpty()) {
            // Show empty state
            binding.sensorRecyclerView.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            // Show sensor grid
            binding.sensorRecyclerView.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            sensorAdapter.submitList(sensorsForGrid)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
