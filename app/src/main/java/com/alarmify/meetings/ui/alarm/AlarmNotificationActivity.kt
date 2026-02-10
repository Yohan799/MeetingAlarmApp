package com.alarmify.meetings.ui.alarm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.alarmify.meetings.alarm.AlarmService
import com.alarmify.meetings.alarm.NativeAlarmManager
import com.alarmify.meetings.databinding.ActivityAlarmNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmNotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmNotificationBinding
    private var eventId: String = ""
    private var eventTitle: String = ""
    private var startTime: Long = 0L
    private var meetingLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show on lock screen and turn screen on
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        binding = ActivityAlarmNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extractIntentData()
        setupUI()
        setupClickListeners()
    }

    private fun extractIntentData() {
        eventId = intent.getStringExtra(NativeAlarmManager.EXTRA_EVENT_ID) ?: ""
        eventTitle = intent.getStringExtra(NativeAlarmManager.EXTRA_EVENT_TITLE) ?: "Meeting"
        startTime = intent.getLongExtra(NativeAlarmManager.EXTRA_EVENT_START_TIME, 0L)
        meetingLink = intent.getStringExtra(NativeAlarmManager.EXTRA_MEETING_LINK)
    }

    private fun setupUI() {
        binding.tvEventTitle.text = eventTitle
        
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        
        binding.tvEventTime.text = timeFormat.format(Date(startTime))
        binding.tvEventDate.text = dateFormat.format(Date(startTime))
        
        // Show meeting link and join button if available
        if (!meetingLink.isNullOrBlank()) {
            binding.tvMeetingLink.visibility = View.VISIBLE
            binding.btnJoinMeeting.visibility = View.VISIBLE
            
            // Determine the meeting type from the link
            val linkText = when {
                meetingLink!!.contains("meet.google.com") -> "Join via Google Meet"
                meetingLink!!.contains("zoom.us") -> "Join via Zoom"
                meetingLink!!.contains("teams.microsoft.com") -> "Join via Microsoft Teams"
                meetingLink!!.contains("webex.com") -> "Join via Webex"
                else -> "Join Meeting"
            }
            binding.tvMeetingLink.text = linkText
        } else {
            binding.tvMeetingLink.visibility = View.GONE
            binding.btnJoinMeeting.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnDismiss.setOnClickListener {
            dismissAlarm()
        }

        binding.btnSnooze.setOnClickListener {
            snoozeAlarm()
        }
        
        binding.btnJoinMeeting.setOnClickListener {
            joinMeeting()
        }
        
        binding.tvMeetingLink.setOnClickListener {
            joinMeeting()
        }
    }

    private fun dismissAlarm() {
        // Stop the alarm service
        val serviceIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        startService(serviceIntent)
        finish()
    }

    private fun snoozeAlarm() {
        // Snooze the alarm for 5 minutes
        val serviceIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
            putExtra(NativeAlarmManager.EXTRA_EVENT_ID, eventId)
            putExtra(NativeAlarmManager.EXTRA_EVENT_TITLE, eventTitle)
            putExtra(NativeAlarmManager.EXTRA_EVENT_START_TIME, startTime)
            putExtra(NativeAlarmManager.EXTRA_MEETING_LINK, meetingLink)
        }
        startService(serviceIntent)
        finish()
    }
    
    private fun joinMeeting() {
        meetingLink?.let { link ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                startActivity(intent)
                // Dismiss the alarm after joining
                dismissAlarm()
            } catch (e: Exception) {
                // If we can't open the link, just dismiss
                dismissAlarm()
            }
        }
    }

    override fun onBackPressed() {
        // Prevent back button from dismissing alarm
        // User must explicitly dismiss or snooze
    }
}
