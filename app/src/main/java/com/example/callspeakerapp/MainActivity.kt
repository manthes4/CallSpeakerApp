package com.example.callspeakerapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.callspeakerapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDrawer()

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = binding.swipeRefreshLayout
        configureSwipeRefreshLayout()

        swipeRefreshLayout.setOnRefreshListener {
            // Simulate a refresh with delay
            refreshContentWithDelay()
        }

        // Setup navigation view listener
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_battery_cpu -> loadFragment(CPUStatsFragment())
                R.id.nav_speaker_mode -> loadFragment(SpeakerModeFragment())
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Default to showing the Battery & CPU section
        loadFragment(CPUStatsFragment())
    }

    private fun setupDrawer() {
        setSupportActionBar(binding.toolbar)

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }

    // Simulates a refresh with a delay so the spinner is visible
    private fun refreshContentWithDelay() {
        // Simulate a delay (e.g., 2 seconds) to let the spinner rotate
        Handler(Looper.getMainLooper()).postDelayed({
            refreshContent() // Trigger the actual refresh
            swipeRefreshLayout.isRefreshing = false // Stop the refresh animation
        }, 600) // Delay in milliseconds (2 seconds)
    }

    private fun refreshContent() {
        // Trigger a refresh. You can reload data or refresh the fragment here.
        loadFragment(CPUStatsFragment()) // Example: Reloading the CPUStatsFragment
    }

    private fun configureSwipeRefreshLayout() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight = displayMetrics.heightPixels

        // Set the offset so that the swipe indicator goes up to 1/5 of the screen height
        val startOffset = 0 // Initial position
        val endOffset = (screenHeight / 5) // 1/5 of screen height

        swipeRefreshLayout.setProgressViewOffset(true, startOffset, endOffset)

        // Customize the progress spinner color
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
