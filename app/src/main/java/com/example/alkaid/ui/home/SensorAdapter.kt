package com.example.alkaid.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.alkaid.data.repository.BaseSensorRepository
import com.example.alkaid.data.sensor.SensorType
import com.example.alkaid.ui.components.SensorCardView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * RecyclerView adapter for displaying sensor cards in a grid layout.
 * Each ViewHolder manages its own sensor data subscription.
 */
class SensorAdapter(
    private val sensorRepositories: Map<SensorType, BaseSensorRepository<*>>,
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<SensorType, SensorAdapter.SensorViewHolder>(SensorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        val sensorCardView = SensorCardView(parent.context)
        sensorCardView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return SensorViewHolder(sensorCardView, lifecycleOwner.lifecycleScope)
    }

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        val sensorType = getItem(position)
        val repository = sensorRepositories[sensorType]
        holder.bind(sensorType, repository)
    }

    override fun onViewRecycled(holder: SensorViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    /**
     * ViewHolder for individual sensor cards.
     */
    class SensorViewHolder(
        private val sensorCardView: SensorCardView,
        private val lifecycleScope: LifecycleCoroutineScope
    ) : RecyclerView.ViewHolder(sensorCardView) {

        private var sensorDataJob: Job? = null

        fun bind(sensorType: SensorType, repository: BaseSensorRepository<*>?) {
            // Setup the sensor card UI
            sensorCardView.setupSensor(sensorType)

            // Cancel any existing subscription
            sensorDataJob?.cancel()

            if (repository != null) {
                // Subscribe to sensor data updates
                sensorDataJob = repository.getSensorData()
                    .onEach { sensorResult ->
                        sensorCardView.updateSensorResult(sensorResult)
                    }
                    .launchIn(lifecycleScope)
            } else {
                // Repository not available, show error
                sensorCardView.updateSensorResult(
                    com.example.alkaid.data.sensor.SensorResult.Error(
                        "Sensor repository not available"
                    )
                )
            }
        }

        fun unbind() {
            // Cancel the sensor data subscription to prevent memory leaks
            sensorDataJob?.cancel()
            sensorDataJob = null
        }
    }

    /**
     * DiffUtil callback for efficient RecyclerView updates.
     */
    private class SensorDiffCallback : DiffUtil.ItemCallback<SensorType>() {
        override fun areItemsTheSame(oldItem: SensorType, newItem: SensorType): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: SensorType, newItem: SensorType): Boolean {
            return oldItem == newItem
        }
    }
}
