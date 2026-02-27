package com.alarmify.meetings.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alarmify.meetings.R
import com.alarmify.meetings.data.auth.FathomAuthManager
import com.alarmify.meetings.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fathomAuthManager: FathomAuthManager
    
    private val homeFragment = HomeFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        fathomAuthManager = FathomAuthManager(this)

        setupNavigation()
        
        // Handle OAuth callback if app was launched via deep link
        handleAuthIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }
    
    private fun handleAuthIntent(intent: Intent?) {
        intent?.let {
            lifecycleScope.launch {
                if (fathomAuthManager.handleAuthResponse(it)) {
                    Toast.makeText(this@MainActivity, "Connected to Fathom AI!", Toast.LENGTH_SHORT).show()
                    
                    // Refresh data in relevant fragments
                    if (homeFragment.isVisible) {
                        homeFragment.loadCalendarEvents()
                    }
                    // If settings fragment is visible, it will auto-update on resume
                }
            }
        }
    }

    private fun setupNavigation() {
        // Set initial fragment
        setCurrentFragment(homeFragment)

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> setCurrentFragment(homeFragment)
                R.id.nav_settings -> setCurrentFragment(settingsFragment)
            }
            true
        }
    }

    private fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainer, fragment)
            commit()
        }
    }
}
