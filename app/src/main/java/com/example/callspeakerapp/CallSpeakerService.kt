package com.example.callspeakerapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat

class CallSpeakerService : Service() {

    private lateinit var callStateListener: CallStateListener
    private var isServiceRunning = false
    private var audioManager: AudioManager? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        callStateListener = CallStateListener(this)
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_SERVICE" -> {
                isServiceRunning = true
                startForegroundService()
                activateSpeakerMode() // Ενεργοποίηση ηχείου όταν ξεκινά η υπηρεσία
            }
            "STOP_SERVICE" -> {
                isServiceRunning = false
                deactivateSpeakerMode() // Απενεργοποίηση ηχείου όταν σταματά η υπηρεσία
                stopForegroundService()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "CallSpeakerServiceChannel",
                "Call Speaker Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)

            val notification: Notification = NotificationCompat.Builder(this, "CallSpeakerServiceChannel")
                .setContentTitle("Call Speaker Service")
                .setContentText("Running...")
                .setSmallIcon(R.drawable.ic_notification)
                .build()

            startForeground(1, notification)
        }
    }

    private fun stopForegroundService() {
        stopForeground(true)
    }

    private fun activateSpeakerMode() {
        audioManager?.let {
            it.isSpeakerphoneOn = true
            it.setStreamVolume(
                AudioManager.STREAM_VOICE_CALL,
                it.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                AudioManager.FLAG_SHOW_UI
            )
            Log.d("CallSpeakerService", "Speaker mode activated manually")
        }
    }

    private fun deactivateSpeakerMode() {
        audioManager?.let {
            it.isSpeakerphoneOn = false
            Log.d("CallSpeakerService", "Speaker mode deactivated manually")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_NONE)
    }

    private class CallStateListener(private val context: Context) : PhoneStateListener() {

        private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.d("CallStateListener", "Call answered. Waiting before activating speaker...")

                    // Χρήση Handler για καθυστέρηση (π.χ. 5 δευτερόλεπτα ~= 3 χτυπήματα)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        Log.d("CallStateListener", "Activating speakerphone now.")
                        audioManager.isSpeakerphoneOn = true
                        audioManager.mode = AudioManager.MODE_IN_CALL // Σημαντικό για να δουλέψει σε πολλές συσκευές

                        audioManager.setStreamVolume(
                            AudioManager.STREAM_VOICE_CALL,
                            audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                            AudioManager.FLAG_SHOW_UI
                        )
                    }, 5000) // 5000 milliseconds = 5 δευτερόλεπτα
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    Log.d("CallStateListener", "Call ended. Deactivating speakerphone.")
                    audioManager.isSpeakerphoneOn = false
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
            }
        }
    }
}
