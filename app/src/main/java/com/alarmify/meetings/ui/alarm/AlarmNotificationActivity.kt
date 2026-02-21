package com.alarmify.meetings.ui.alarm

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
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
    
    // Animation holders
    private val pulseAnimators = mutableListOf<ObjectAnimator>()

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
        startPulseAnimation()
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
        val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        
        binding.tvEventTime.text = timeFormat.format(Date(startTime))
        binding.tvEventDate.text = dateFormat.format(Date(startTime))
        
        // Show meeting link and join button if available
        if (!meetingLink.isNullOrBlank()) {
            binding.btnJoinMeeting.visibility = View.VISIBLE
            binding.btnJoinMeeting.text = "Join now"
            binding.btnJoinMeeting.setOnClickListener {
                joinMeeting()
            }
        } else {
            binding.btnJoinMeeting.visibility = View.GONE
        }
    }
    
    private fun startPulseAnimation() {
        // Create pulsing effect for rings
        val rings = listOf(binding.pulseRing1, binding.pulseRing2, binding.pulseRing3)
        
        rings.forEachIndexed { index, view ->
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.2f, 1f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.2f, 1f)
            val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.3f, 0.1f, 0.3f)
            
            val animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha).apply {
                duration = 2000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                startDelay = index * 300L
                start()
            }
            pulseAnimators.add(animator)
        }
        
        // Animate icon punch
        val iconScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, 1f)
        val iconScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.1f, 1f)
        ObjectAnimator.ofPropertyValuesHolder(binding.ivAlarmIcon, iconScaleX, iconScaleY).apply {
             duration = 1000
             repeatCount = ObjectAnimator.INFINITE
             interpolator = AccelerateDecelerateInterpolator()
             start()
             pulseAnimators.add(this)
        }
    }

    private fun setupClickListeners() {
        binding.btnDismiss.setOnClickListener {
            dismissAlarm()
        }

        binding.btnSnooze.setOnClickListener {
            snoozeAlarm()
        }
    }

    private fun dismissAlarm() {
        stopAnimations()
        val serviceIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        startService(serviceIntent)
        finishAndRemoveTask()
    }

    private fun snoozeAlarm() {
        stopAnimations()
        // Snooze the alarm for 5 minutes
        val serviceIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
            putExtra(NativeAlarmManager.EXTRA_EVENT_ID, eventId)
            putExtra(NativeAlarmManager.EXTRA_EVENT_TITLE, eventTitle)
            putExtra(NativeAlarmManager.EXTRA_EVENT_START_TIME, startTime)
            putExtra(NativeAlarmManager.EXTRA_MEETING_LINK, meetingLink)
        }
        startService(serviceIntent)
        finishAndRemoveTask()
    }
    
    private fun joinMeeting() {
        meetingLink?.let { link ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                startActivity(intent)
                dismissAlarm()
            } catch (e: Exception) {
                dismissAlarm()
            }
        }
    }
    
    private fun stopAnimations() {
        pulseAnimators.forEach { it.cancel() }
        pulseAnimators.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAnimations()
    }

    override fun onBackPressed() {
        // Prevent back button
    }
}
