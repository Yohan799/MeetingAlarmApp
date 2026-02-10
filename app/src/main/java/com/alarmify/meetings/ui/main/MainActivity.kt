package com.alarmify.meetings.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.alarmify.meetings.R
import com.alarmify.meetings.alarm.NativeAlarmManager
import com.alarmify.meetings.data.model.CalendarEvent
import com.alarmify.meetings.data.repository.CalendarRepository
import com.alarmify.meetings.databinding.ActivityMainBinding
import com.alarmify.meetings.ui.adapter.EventsAdapter
import com.alarmify.meetings.ui.auth.SignInActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var calendarRepository: CalendarRepository
    private lateinit var nativeAlarmManager: NativeAlarmManager
    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var googleSignInClient: GoogleSignInClient
    
    private val events = mutableListOf<CalendarEvent>()
    
    // Default alarm times in minutes
    private val defaultAlarmTimes = listOf(5, 10, 15, 30, 60)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupRecyclerView()
        setupClickListeners()
        checkExactAlarmPermission()
        loadCalendarEvents()
    }

    private fun initializeComponents() {
        calendarRepository = CalendarRepository(this)
        nativeAlarmManager = NativeAlarmManager(this)
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupRecyclerView() {
        eventsAdapter = EventsAdapter(
            events = events,
            onAlarmClick = { event -> showAlarmOptionsDialog(event) },
            onCancelAlarmClick = { event -> cancelAlarm(event) }
        )
        
        binding.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = eventsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnRefresh.setOnClickListener {
            loadCalendarEvents()
        }
        
        binding.btnSignOut.setOnClickListener {
            signOut()
        }
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadCalendarEvents()
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!nativeAlarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("This app needs permission to set exact alarms for meeting reminders.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun loadCalendarEvents() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateContainer.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val fetchedEvents = calendarRepository.fetchUpcomingEvents()
                events.clear()
                events.addAll(fetchedEvents)
                
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    
                    if (events.isEmpty()) {
                        binding.emptyStateContainer.visibility = View.VISIBLE
                        binding.recyclerViewEvents.visibility = View.GONE
                    } else {
                        binding.emptyStateContainer.visibility = View.GONE
                        binding.recyclerViewEvents.visibility = View.VISIBLE
                        eventsAdapter.notifyDataSetChanged()
                    }
                    
                    Toast.makeText(
                        this@MainActivity,
                        "Loaded ${events.size} events",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading events: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showAlarmOptionsDialog(event: CalendarEvent) {
        val options = defaultAlarmTimes.map { "$it minutes before" }.toMutableList()
        options.add("Custom")

        AlertDialog.Builder(this)
            .setTitle("Set alarm for ${event.title}")
            .setItems(options.toTypedArray()) { _, which ->
                if (which < defaultAlarmTimes.size) {
                    setAlarm(event, defaultAlarmTimes[which])
                } else {
                    showCustomAlarmDialog(event)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomAlarmDialog(event: CalendarEvent) {
        // Create a container with padding for the EditText
        val container = android.widget.FrameLayout(this).apply {
            setPadding(48, 24, 48, 0)
        }
        
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Minutes before meeting"
            // Set text and hint colors explicitly for proper visibility
            setTextColor(android.graphics.Color.BLACK)
            setHintTextColor(android.graphics.Color.GRAY)
            setBackgroundResource(android.R.drawable.edit_text)
            setPadding(24, 24, 24, 24)
        }
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Custom Alarm Time")
            .setView(container)
            .setPositiveButton("Set") { _, _ ->
                val minutes = input.text.toString().toIntOrNull()
                if (minutes != null && minutes > 0) {
                    setAlarm(event, minutes)
                } else {
                    Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setAlarm(event: CalendarEvent, minutesBefore: Int) {
        try {
            nativeAlarmManager.setAlarm(event, minutesBefore)
            
            // Update the event in the list
            val index = events.indexOfFirst { it.id == event.id }
            if (index >= 0) {
                events[index] = event.copy(
                    isAlarmSet = true,
                    alarmMinutesBefore = minutesBefore
                )
                eventsAdapter.notifyItemChanged(index)
            }
            
            Toast.makeText(
                this,
                "Alarm set for $minutesBefore minutes before ${event.title}",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Failed to set alarm: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun cancelAlarm(event: CalendarEvent) {
        nativeAlarmManager.cancelAlarm(event.id)
        
        val index = events.indexOfFirst { it.id == event.id }
        if (index >= 0) {
            events[index] = event.copy(
                isAlarmSet = false,
                alarmMinutesBefore = 0
            )
            eventsAdapter.notifyItemChanged(index)
        }
        
        Toast.makeText(this, "Alarm cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun signOut() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Yes") { _, _ ->
                googleSignInClient.signOut().addOnCompleteListener {
                    startActivity(Intent(this, SignInActivity::class.java))
                    finish()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
