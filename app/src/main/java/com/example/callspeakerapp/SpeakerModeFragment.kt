package com.example.callspeakerapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.callspeakerapp.databinding.FragmentSpeakerModeBinding

class SpeakerModeFragment : Fragment() {

    private var _binding: FragmentSpeakerModeBinding? = null
    private val binding get() = _binding!!

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpeakerModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Launcher για τις άδειες
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startService()
            } else {
                Log.e("SpeakerModeFragment", "Permission denied")
            }
        }

        // Κουμπί Start
        binding.btnStart.setOnClickListener {
            checkPermissionsAndStartService()
        }

        // Κουμπί Stop
        binding.btnStop.setOnClickListener {
            stopService()
        }
    }

    private fun checkPermissionsAndStartService() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED -> {
                startService()
            }
            else -> {
                // Αίτηση για άδεια
                requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            }
        }
    }

    private fun startService() {
        Log.d("SpeakerModeFragment", "Starting foreground service")
        val serviceIntent = Intent(requireContext(), CallSpeakerService::class.java).apply {
            action = "START_SERVICE"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(requireContext(), serviceIntent)
        } else {
            requireActivity().startService(serviceIntent)
        }
    }

    private fun stopService() {
        Log.d("SpeakerModeFragment", "Attempting to stop service")
        val serviceIntent = Intent(requireContext(), CallSpeakerService::class.java).apply {
            action = "STOP_SERVICE"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(requireContext(), serviceIntent)
        } else {
            requireActivity().startService(serviceIntent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
