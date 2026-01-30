package com.bvalosek.cpuspy

import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import android.os.SystemClock
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

class CpuStateMonitor {

    private val TIME_IN_STATE_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state"
    private val TAG = "CpuStateMonitor"

    private var _states: MutableList<CpuState> = ArrayList()
    private var _offsets: MutableMap<Int, Long> = HashMap()

    inner class CpuStateMonitorException(message: String) : Exception(message)

    data class CpuState(var freq: Int = 0, var duration: Long = 0) : Comparable<CpuState> {
        override fun compareTo(other: CpuState): Int {
            return freq.compareTo(other.freq)
        }
    }

    fun getStates(): List<CpuState> {
        val states = ArrayList<CpuState>()
        for (state in _states) {
            var duration = state.duration
            _offsets[state.freq]?.let {
                if (it <= duration) {
                    duration -= it
                } else {
                    _offsets.clear()
                    return getStates()
                }
            }
            states.add(CpuState(state.freq, duration))
        }
        return states
    }

    fun getTotalStateTime(): Long {
        var sum: Long = 0
        var offset: Long = 0

        for (state in _states) {
            sum += state.duration
        }

        for (entry in _offsets.entries) {
            offset += entry.value
        }

        return sum - offset
    }

    fun getOffsets(): Map<Int, Long> {
        return _offsets
    }

    fun setOffsets(offsets: Map<Int, Long>) {
        _offsets = offsets.toMutableMap()
    }

    @Throws(CpuStateMonitorException::class)
    fun setOffsets() {
        _offsets.clear()
        updateStates()
        for (state in _states) {
            _offsets[state.freq] = state.duration
        }
    }

    fun removeOffsets() {
        _offsets.clear()
    }

    @Throws(CpuStateMonitorException::class)
    fun updateStates(): List<CpuState> {
        try {
            FileInputStream(TIME_IN_STATE_PATH).use { `is` ->
                InputStreamReader(`is`).use { ir ->
                    BufferedReader(ir).use { br ->
                        _states.clear()
                        readInStates(br)
                    }
                }
            }
        } catch (e: IOException) {
            throw CpuStateMonitorException("Problem opening time-in-states file")
        }

        val sleepTime = (SystemClock.elapsedRealtime() - SystemClock.uptimeMillis()) / 10
        _states.add(CpuState(0, sleepTime))
        _states.sortWith(compareByDescending { it.freq })

        return _states
    }

    @Throws(CpuStateMonitorException::class)
    private fun readInStates(br: BufferedReader) {
        try {
            var line: String?
            while (br.readLine().also { line = it } != null) {
                val nums = line!!.split(" ")
                _states.add(CpuState(nums[0].toInt(), nums[1].toLong()))
            }
        } catch (e: IOException) {
            throw CpuStateMonitorException("Problem processing time-in-states file")
        }
    }
}