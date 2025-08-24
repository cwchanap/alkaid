package com.example.alkaid.ui.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.alkaid.R
import com.example.alkaid.data.weather.WeatherDisplayData
import com.example.alkaid.databinding.FragmentWeatherBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Fragment that displays weather information for current location
 */
class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: WeatherViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[WeatherViewModel::class.java]

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        // Observe weather state changes
        viewModel.weatherState
            .onEach { state ->
                updateUI(state)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
            
        // Observe location changes for display
        viewModel.currentLocation
            .onEach { location ->
                location?.let {
                    binding.locationText.text = it.getFormattedLatLng()
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setupClickListeners() {
        // Refresh button
        binding.btnRefresh.setOnClickListener {
            viewModel.refreshWeather()
        }
        
        // Retry button (in error state)
        binding.btnRetry.setOnClickListener {
            viewModel.refreshWeather()
        }
        
        // Go to settings button (in setup state)
        binding.btnGoToSettings.setOnClickListener {
            findNavController().navigate(R.id.navigation_settings)
        }
    }

    private fun updateUI(state: WeatherViewState) {
        // Hide all states first
        hideAllStates()
        
        when (state) {
            is WeatherViewState.CheckingApiKey -> {
                binding.loadingProgress.visibility = View.VISIBLE
            }
            is WeatherViewState.NoApiKey -> {
                binding.setupLayout.visibility = View.VISIBLE
            }
            is WeatherViewState.WaitingForLocation -> {
                binding.loadingProgress.visibility = View.VISIBLE
            }
            is WeatherViewState.Loading -> {
                binding.loadingProgress.visibility = View.VISIBLE
                if (binding.weatherContent.visibility == View.VISIBLE) {
                    // Keep weather content visible during refresh
                    binding.loadingProgress.visibility = View.GONE
                }
            }
            is WeatherViewState.Success -> {
                binding.weatherContent.visibility = View.VISIBLE
                updateWeatherContent(state.weatherData)
            }
            is WeatherViewState.Error -> {
                binding.errorLayout.visibility = View.VISIBLE
                binding.errorMessage.text = state.message
            }
        }
    }

    private fun hideAllStates() {
        binding.loadingProgress.visibility = View.GONE
        binding.setupLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
        binding.weatherContent.visibility = View.GONE
    }

    private fun updateWeatherContent(weatherData: WeatherDisplayData) {
        with(binding) {
            // Location already updated by location observer
            
            // Main weather info
            temperature.text = weatherData.temperature
            weatherCondition.text = weatherData.condition
            feelsLike.text = weatherData.feelsLike
            
            // Use default weather icon for now
            // TODO: Add image loading library like Glide or Coil to load weather icons from URL
            weatherIcon.setImageResource(R.drawable.ic_weather_black_24dp)
            
            // Weather details
            humidityValue.text = weatherData.humidity
            pressureValue.text = weatherData.pressure
            windValue.text = weatherData.windSpeed
            visibilityValue.text = weatherData.visibility
            
            // Last updated
            lastUpdated.text = getString(R.string.weather_last_updated, weatherData.lastUpdated)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
