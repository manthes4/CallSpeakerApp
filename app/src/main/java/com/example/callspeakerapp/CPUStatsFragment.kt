package com.example.callspeakerapp

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bvalosek.cpuspy.CpuStateMonitor
import com.bvalosek.cpuspy.CpuStateMonitor.CpuState
import com.bvalosek.cpuspy.CpuStateMonitor.CpuStateMonitorException
import com.example.callspeakerapp.databinding.FragmentCpuStatsBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.PrintWriter
import kotlin.text.clear

class CPUStatsFragment : Fragment() {

    private lateinit var cpuStatsTextView: TextView
    private lateinit var cpuStateMonitor: CpuStateMonitor
    private var _binding: FragmentCpuStatsBinding? = null
    private val binding get() = _binding!!

    // Map to store the offsets for each frequency
    private var stateOffsets: MutableMap<Int, Long> = mutableMapOf()

    private val fileName = "cpu_state_offsets.json"
    private val refreshInterval = 20000L  // 20 seconds
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateStats()
            loadCPUStates()
            handler.postDelayed(this, refreshInterval)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCpuStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cpuStatsTextView = binding.cpuStatsTextView

        // Initialize CpuStateMonitor
        cpuStateMonitor = CpuStateMonitor()

        // Load CPU states and update stats
        loadOffsets()
        loadCPUStates()

        // Display battery stats and screen-on time
        updateStats()

        // Set up button to reset battery stats
        view.findViewById<Button>(R.id.resetBatteryButton).setOnClickListener {
            resetBatteryStats()
            updateStats()
        }

        // Set up button to reset CPU stats (simulate reset using offsets)
        view.findViewById<Button>(R.id.resetCpuButton).setOnClickListener {
            resetCpuStats()
            updateStats()
        }
    }

    private fun loadCPUStates() {
        try {
            // Fetch CPU states from the monitor
            val cpuStates: List<CpuState> = cpuStateMonitor.updateStates()
            val totalStateTime = cpuStateMonitor.getTotalStateTime()

            // Prepare the string to display the CPU states
            val sb = StringBuilder()
            sb.append("CPU States:\n\n")

            for (state in cpuStates) {
                val adjustedDuration = getAdjustedDuration(state)
                val freq = if (state.freq == 0) "Deep Sleep" else "${formatFrequency(state.freq)} MHz"
                val duration = formatDuration(adjustedDuration)

                // Create a formatted string with extra space
                val formattedString = String.format("%-15s%s", freq, duration)

                sb.append("$formattedString\n")
            }

            sb.append("\nTotal time: ${formatDuration(totalStateTime - getTotalOffset())}")

            // Set the text view with CPU states information
            cpuStatsTextView.text = sb.toString()

        } catch (e: CpuStateMonitorException) {
            e.printStackTrace()
            cpuStatsTextView.text = "Failed to retrieve CPU states."
        }
    }

    // Format frequency from Hz to MHz
    private fun formatFrequency(freqHz: Int): Int {
        return freqHz / 1000
    }

    private fun updateStats() {
        binding.screenOnTimeTextView.text = getScreenOnTime()
        binding.batteryStatsTextView.text = getBatteryStats()
    }

    private fun getBatteryStats(): String {
        val batteryStatus = activity?.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        return "Battery Level: $level%"
    }

    private fun getScreenOnTime(): String {
        return try {
            val process = Runtime.getRuntime().exec("su -c dumpsys batterystats")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?

            // Read the output and find the "Screen on" time
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("Screen on:", ignoreCase = true) == true) {
                    output.append(line!!.substringAfter("Screen on:").trim()).append("\n")
                }
            }
            reader.close()

            // Extract time in format "Xh Ym Zs"
            val screenOnTime = output.toString().trim()
            val timeRegex = Regex("(\\d+h)?\\s*(\\d+m)?\\s*(\\d+s)?")
            val matchResult = timeRegex.find(screenOnTime)

            if (matchResult != null) {
                "Screen On time: " + matchResult.value.trim()
            } else {
                "Screen On time: not found"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Failed to get screen-on time."
        }
    }

    private fun resetBatteryStats() {
        try {
            Runtime.getRuntime().exec("su -c dumpsys batterystats --reset")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resetCpuStats() {
        try {
            // Update and store the current states as offsets
            val cpuStates: List<CpuState> = cpuStateMonitor.updateStates()
            stateOffsets.clear()
            for (state in cpuStates) {
                stateOffsets[state.freq] = state.duration
            }

            // Save the offsets to file
            saveOffsets()

            // Update the CPU states and UI after the reset
            loadCPUStates()
            updateStats()
        } catch (e: CpuStateMonitorException) {
            e.printStackTrace()
        }
    }

    // Method to retrieve adjusted duration for each CPU state using offsets
    private fun getAdjustedDuration(state: CpuState): Long {
        val offset = stateOffsets[state.freq] ?: 0L
        return maxOf(state.duration - offset, 0L)  // Ensure the duration is not negative
    }

    // Save offsets to a JSON file
    private fun saveOffsets() {
        try {
            val file = File(requireContext().filesDir, fileName)
            PrintWriter(FileWriter(file)).use { writer ->
                val gson = Gson()
                val json = gson.toJson(stateOffsets)
                writer.write(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Load offsets from a JSON file
    private fun loadOffsets() {
        try {
            val file = File(requireContext().filesDir, fileName)
            if (file.exists()) {
                // Check if the device has recently restarted
                if (SystemClock.elapsedRealtime() < 60 * 1000) { // Less than 1 minute of uptime
                    // Device has likely restarted, clear the offsets
                    stateOffsets.clear()
                    saveOffsets() // Save the cleared offsets
                } else {
                    // Load offsets from file
                    BufferedReader(FileReader(file)).use { reader ->
                        val gson = Gson()
                        val type = object : TypeToken<MutableMap<Int, Long>>() {}.type
                        val offsets: MutableMap<Int, Long> = gson.fromJson(reader, type)
                        stateOffsets = offsets
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Get total offset for all states (for the total time display)
    private fun getTotalOffset(): Long {
        var totalOffset = 0L
        for (offset in stateOffsets.values) {
            totalOffset += offset
        }
        return totalOffset
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 100
        val minutes = seconds / 60
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d:%02d", hours, remainingMinutes, remainingSeconds)
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        handler.removeCallbacks(refreshRunnable)  // Ensure the handler is stopped when view is destroyed
    }
}
