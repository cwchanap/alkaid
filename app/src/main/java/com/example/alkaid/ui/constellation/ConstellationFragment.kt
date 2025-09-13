
package com.example.alkaid.ui.constellation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.alkaid.R
import com.example.alkaid.databinding.FragmentConstellationBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConstellationFragment : Fragment() {

    private var _binding: FragmentConstellationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConstellationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConstellationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.constellations.collectLatest {
                binding.constellationMapView.setConstellations(it)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.location.collectLatest {
                it?.let {
                    binding.constellationMapView.setLocation(it.latitude, it.longitude)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
