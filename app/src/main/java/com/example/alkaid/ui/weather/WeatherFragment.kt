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
        return try {
            android.util.Log.d("WeatherFragment", "Creating view with binding")
            _binding = FragmentWeatherBinding.inflate(inflater, container, false)
            android.util.Log.d("WeatherFragment", "Binding created successfully")
            binding.root
        } catch (e: Exception) {
            android.util.Log.e("WeatherFragment", "Failed to create binding, using fallback", e)
            // Fallback: create a simple view
            createFallbackView(inflater, container)
        }
    }
    
    private fun createFallbackView(inflater: LayoutInflater, container: ViewGroup?): View {
        return try {
            // Try to inflate the layout directly without binding
            inflater.inflate(R.layout.fragment_weather, container, false)
        } catch (e: Exception) {
            android.util.Log.e("WeatherFragment", "Failed to inflate layout, creating simple view", e)
            // Ultimate fallback: create a simple TextView
            android.widget.TextView(requireContext()).apply {
                text = "Weather functionality temporarily unavailable.\nPlease check Settings to configure your API key."
                textSize = 16f
                setPadding(32, 32, 32, 32)
                gravity = android.view.Gravity.CENTER
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            android.util.Log.d("WeatherFragment", "onViewCreated called")
            
            // Check if we have binding available
            if (_binding == null) {
                android.util.Log.w("WeatherFragment", "No binding available, weather functionality limited")
                return
            }
            
            // Show setup state immediately to prevent crashes
            showSetupState()
            
            // Try to initialize ViewModel in a safer way
            initializeViewModelSafely()
        } catch (e: Exception) {
            android.util.Log.e("WeatherFragment", "Critical error in onViewCreated", e)
            showCriticalError(e)
        }
    }
    
    private fun showSetupState() {
        try {
            _binding?.let { binding ->
                binding.setupLayout.visibility = View.VISIBLE
                binding.loadingProgress.visibility = View.GONE
                binding.errorLayout.visibility = View.GONE
                binding.weatherContent.visibility = View.GONE
            } ?: android.util.Log.w("WeatherFragment", "Cannot show setup state - binding is null")
        } catch (e: Exception) {
            android.util.Log.e("WeatherFragment", "Error showing setup state", e)
        }
    }
    
    private fun showCriticalError(error: Exception) {
        try {
            _binding?.let { binding ->
                binding.errorLayout.visibility = View.VISIBLE
                binding.errorMessage.text = "Critical error: ${error.message}"
                binding.loadingProgress.visibility = View.GONE
                binding.setupLayout.visibility = View.GONE
                binding.weatherContent.visibility = View.GONE
            } ?: android.util.Log.w("WeatherFragment", "Cannot show critical error - binding is null")
        } catch (e: Exception) {
            android.util.Log.e("WeatherFragment", "Failed to show critical error", e)
        }
    }
    
    private fun initializeViewModelSafely() {
        try {
            viewModel = ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
            )[WeatherViewModel::class.java]

            setupObservers()
            setupClickListeners()
        } catch (e: Exception) {
            android.util.Log.e("WeatherFragment", "Failed to initialize ViewModel", e)
            binding.errorLayout.visibility = View.VISIBLE
            binding.errorMessage.text = "Failed to initialize weather: ${e.message}"
            hideOtherStates()
        }
    }

    private fun setupObservers() {
        try {
            if (!::viewModel.isInitialized) {
                android.util.Log.e("WeatherFragment", "ViewModel not initialized")
                return
            }
            
            // Observe weather state changes
            viewModel.weatherState
                .onEach { state ->
                    try {
                        updateUI(state)
                    } catch (e: Exception) {
                        // Handle any UI update errors gracefully
                        android.util.Log.e("WeatherFragment", "UI update error", e)
                        binding.errorLayout.visibility = View.VISIBLE
                        binding.errorMessage.text = "UI Error: ${e.message}"
                        hideOtherStates()
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
                
            // Observe location changes for display
            viewModel.currentLocation
                .onEach { location ->
                    try {
                        location?.let {
                            binding.locationText.text = it.getFormattedLatLng()
                        } ?: run {
                            binding.locationText.text = "Location not available"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("WeatherFragment", "Location display error", e)
                        binding.locationText.text = "Location error"
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
        } catch (e: Exception) {
            android.util.Log.e("WeatherFragment", "Failed to setup observers", e)
            binding.errorLayout.visibility = View.VISIBLE
            binding.errorMessage.text = "Failed to setup weather observers: ${e.message}"
            hideOtherStates()
        }
    }

    private fun setupClickListeners() {
        try {
            // Refresh button
            binding.btnRefresh.setOnClickListener {
                if (::viewModel.isInitialized) {
                    viewModel.refreshWeather()
                }
            }
            
            // Retry button (in error state)
            binding.btnRetry.setOnClickListener {
                if (::viewModel.isInitialized) {
                    viewModel.refreshWeather()
                }
            }
            
            // Go to settings button (in setup state)
            binding.btnGoToSettings.setOnClickListener {
                try {
                    findNavController().navigate(R.id.navigation_settings)
                } catch (e: Exception) {
                    android.util.Log.e("WeatherFragment", "Navigation error", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("WeatherFragment", "Failed to setup click listeners", e)
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
    
    private fun hideOtherStates() {
        binding.loadingProgress.visibility = View.GONE
        binding.setupLayout.visibility = View.GONE
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
