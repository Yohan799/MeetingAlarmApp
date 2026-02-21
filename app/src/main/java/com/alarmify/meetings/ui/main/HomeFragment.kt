package com.alarmify.meetings.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alarmify.meetings.R
import com.alarmify.meetings.alarm.NativeAlarmManager
import com.alarmify.meetings.data.auth.FathomAuthManager
import com.alarmify.meetings.data.model.CalendarEvent
import com.alarmify.meetings.data.repository.CalendarRepository
import com.alarmify.meetings.data.repository.FathomRepository
import com.alarmify.meetings.databinding.FragmentHomeBinding
import com.alarmify.meetings.debug.CrashLogger
import com.alarmify.meetings.ui.adapter.EventsAdapter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeParseException

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarRepository: CalendarRepository
    private lateinit var nativeAlarmManager: NativeAlarmManager
    private lateinit var eventsAdapter: EventsAdapter
    
    // Fathom Integration
    private lateinit var fathomAuthManager: FathomAuthManager
    private lateinit var fathomRepository: FathomRepository

    private val events = mutableListOf<CalendarEvent>()
    
    // Default alarm times in minutes
    private val defaultAlarmTimes = listOf(5, 10, 15, 30, 60)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            CrashLogger.logDebug(requireContext(), "Home", "HomeFragment.onViewCreated started")
            initializeComponents()
            CrashLogger.logDebug(requireContext(), "Home", "Components initialized")
            setupRecyclerView()
            CrashLogger.logDebug(requireContext(), "Home", "RecyclerView set up")
            setupClickListeners()
            checkExactAlarmPermission()
            CrashLogger.logDebug(requireContext(), "Home", "Starting loadCalendarEvents...")
            loadCalendarEvents()
            CrashLogger.logDebug(requireContext(), "Home", "HomeFragment.onViewCreated completed OK")
        } catch (e: Exception) {
            CrashLogger.logError(requireContext(), "Home-onViewCreated", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeComponents() {
        val context = requireContext()
        calendarRepository = CalendarRepository(context)
        nativeAlarmManager = NativeAlarmManager(context)
        
        fathomAuthManager = FathomAuthManager(context)
        fathomRepository = FathomRepository(fathomAuthManager)
    }

    private fun setupRecyclerView() {
        eventsAdapter = EventsAdapter(
            events = events,
            onAlarmClick = { event -> showAlarmOptionsDialog(event) },
            onCancelAlarmClick = { event -> cancelAlarm(event) },
            onViewNotesClick = { event -> showMeetingSummary(event) }
        )
        
        binding.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnRefresh.setOnClickListener {
            loadCalendarEvents()
        }
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadCalendarEvents()
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!nativeAlarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(requireContext())
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

    fun loadCalendarEvents() {
        if (_binding == null) return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateContainer.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                CrashLogger.logDebug(requireContext(), "Home", "Fetching calendar events...")
                
                val fetchedEvents = calendarRepository.fetchUpcomingEvents()
                CrashLogger.logDebug(requireContext(), "Home", "Fetched ${fetchedEvents.size} events")
                events.clear()
                events.addAll(fetchedEvents)
                
                updateUIState()
                
                // Then try to sync Fathom data in background if connected
                if (fathomAuthManager.isAuthorized()) {
                    try {
                        syncFathomData()
                    } catch (e: Exception) {
                        CrashLogger.logError(requireContext(), "Home-Fathom", e)
                    }
                }
                
            } catch (e: Exception) {
                CrashLogger.logError(requireContext(), "Home-loadEvents", e)
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(
                        requireContext(),
                        "Error loading events: ${e.javaClass.simpleName}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun updateUIState() {
        if (_binding == null) return
        
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
    }
    
    private suspend fun syncFathomData() {
        val fathomMeetings = fathomRepository.getRecentMeetings()
        if (fathomMeetings.isEmpty()) return
        
        var matchedCount = 0
        val updatedEvents = events.map { event ->
            // Try to match Fathom meeting to Calendar event
            val matchingFathomMeeting = fathomMeetings.find { fm ->
                val fmTime = fm.eventTime
                if (fmTime != null) {
                    try {
                        val fmInstant = Instant.parse(fmTime)
                        val eventInstant = Instant.ofEpochMilli(event.startTime)
                        
                        val diffSeconds = Math.abs(fmInstant.epochSecond - eventInstant.epochSecond)
                        diffSeconds < 20 * 60 // 20 minutes
                    } catch (e: DateTimeParseException) {
                        false
                    }
                } else {
                    false
                }
            }
            
            if (matchingFathomMeeting != null) {
                matchedCount++
                event.copy(fathomRecordingId = matchingFathomMeeting.id)
            } else {
                event
            }
        }.toMutableList()
        
        events.clear()
        events.addAll(updatedEvents)
        
        // Post results to main thread
        activity?.runOnUiThread {
             if (_binding != null) {
                updateUIState()
                if (matchedCount > 0) {
                     Toast.makeText(requireContext(), "Found $matchedCount Fathom summaries", Toast.LENGTH_SHORT).show()
                }
             }
        }
    }
    
    private fun showMeetingSummary(event: CalendarEvent) {
        val recordingId = event.fathomRecordingId ?: return
        
        val progressDialog = AlertDialog.Builder(requireContext())
            .setMessage("Fetching summary...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        lifecycleScope.launch {
            try {
                val summary = fathomRepository.getMeetingSummary(recordingId)
                progressDialog.dismiss()
                
                if (summary != null) {
                    SummaryBottomSheet(event, summary).show(parentFragmentManager, "SummaryBottomSheet")
                } else {
                    Toast.makeText(requireContext(), "Failed to load summary", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAlarmOptionsDialog(event: CalendarEvent) {
        val options = defaultAlarmTimes.map { "$it minutes before" }.toMutableList()
        options.add("Custom")

        AlertDialog.Builder(requireContext())
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
        val container = android.widget.FrameLayout(requireContext()).apply {
            setPadding(48, 24, 48, 0)
        }
        
        val input = android.widget.EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Minutes before meeting"
            setTextColor(android.graphics.Color.BLACK)
            setHintTextColor(android.graphics.Color.GRAY)
            setBackgroundResource(android.R.drawable.edit_text)
            setPadding(24, 24, 24, 24)
        }
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Custom Alarm Time")
            .setView(container)
            .setPositiveButton("Set") { _, _ ->
                val minutes = input.text.toString().toIntOrNull()
                if (minutes != null && minutes > 0) {
                    setAlarm(event, minutes)
                } else {
                    Toast.makeText(requireContext(), "Invalid input", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setAlarm(event: CalendarEvent, minutesBefore: Int) {
        try {
            nativeAlarmManager.setAlarm(event, minutesBefore)
            
            val index = events.indexOfFirst { it.id == event.id }
            if (index >= 0) {
                events[index] = event.copy(
                    isAlarmSet = true,
                    alarmMinutesBefore = minutesBefore
                )
                eventsAdapter.notifyItemChanged(index)
            }
            
            Toast.makeText(
                requireContext(),
                "Alarm set for $minutesBefore minutes before ${event.title}",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
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
        
        Toast.makeText(requireContext(), "Alarm cancelled", Toast.LENGTH_SHORT).show()
    }
}
