package com.alarmify.meetings.alarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alarmify.meetings.MeetingAlarmApplication
import com.alarmify.meetings.R
import com.alarmify.meetings.ui.alarm.AlarmNotificationActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private lateinit var alarmManager: AlarmManager
    
    companion object {
        private const val TAG = "AlarmService"
        private const val NOTIFICATION_ID = 1001
        private const val SNOOZE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
        const val ACTION_DISMISS = "com.alarmify.meetings.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.alarmify.meetings.ACTION_SNOOZE"
    }

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AlarmService started")

        val eventId = intent?.getStringExtra(NativeAlarmManager.EXTRA_EVENT_ID) ?: ""
        val eventTitle = intent?.getStringExtra(NativeAlarmManager.EXTRA_EVENT_TITLE) ?: "Meeting"
        val startTime = intent?.getLongExtra(NativeAlarmManager.EXTRA_EVENT_START_TIME, 0L) ?: 0L
        val meetingLink = intent?.getStringExtra(NativeAlarmManager.EXTRA_MEETING_LINK)

        when (intent?.action) {
            ACTION_DISMISS -> {
                dismissAlarm()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                snoozeAlarm(eventId, eventTitle, startTime, meetingLink)
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, createForegroundNotification())
                triggerAlarm(eventId, eventTitle, startTime, meetingLink)
            }
        }

        return START_NOT_STICKY
    }

    private fun triggerAlarm(eventId: String, eventTitle: String, startTime: Long, meetingLink: String?) {
        // Start alarm sound
        playAlarmSound()
        
        // Start vibration
        startVibration()
        
        // Show full screen alarm activity
        val alarmIntent = Intent(this, AlarmNotificationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NativeAlarmManager.EXTRA_EVENT_ID, eventId)
            putExtra(NativeAlarmManager.EXTRA_EVENT_TITLE, eventTitle)
            putExtra(NativeAlarmManager.EXTRA_EVENT_START_TIME, startTime)
            putExtra(NativeAlarmManager.EXTRA_MEETING_LINK, meetingLink)
        }
        startActivity(alarmIntent)
    }

    private fun playAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            Log.d(TAG, "Alarm sound started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play alarm sound: ${e.message}")
        }
    }

    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 500, 500, 500, 500)
            val effect = VibrationEffect.createWaveform(pattern, 0) // 0 means repeat
            vibrator?.vibrate(effect)
            Log.d(TAG, "Vibration started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibration: ${e.message}")
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun dismissAlarm() {
        Log.d(TAG, "Dismissing alarm")
        stopAlarmSound()
        stopVibration()
        stopForeground(true)
        stopSelf()
    }

    private fun snoozeAlarm(eventId: String, eventTitle: String, startTime: Long, meetingLink: String?) {
        Log.d(TAG, "Snoozing alarm for 5 minutes")
        stopAlarmSound()
        stopVibration()
        
        // Reschedule alarm for 5 minutes later
        val snoozeTime = System.currentTimeMillis() + SNOOZE_DURATION_MS
        
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = "com.alarmify.meetings.ALARM_TRIGGER"
            putExtra(NativeAlarmManager.EXTRA_EVENT_ID, eventId)
            putExtra(NativeAlarmManager.EXTRA_EVENT_TITLE, eventTitle)
            putExtra(NativeAlarmManager.EXTRA_EVENT_START_TIME, startTime)
            putExtra(NativeAlarmManager.EXTRA_ALARM_TIME, snoozeTime)
            putExtra(NativeAlarmManager.EXTRA_MEETING_LINK, meetingLink)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            eventId.hashCode() + 1000, // Different request code for snooze
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Snooze alarm scheduled for $snoozeTime")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule snooze alarm: ${e.message}")
        }
        
        stopForeground(true)
        stopSelf()
    }

    private fun createForegroundNotification(): Notification {
        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MeetingAlarmApplication.ALARM_CHANNEL_ID)
            .setContentTitle("Meeting Alarm")
            .setContentText("Meeting alarm is active")
            .setSmallIcon(R.drawable.ic_alarm_ringing)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
        stopVibration()
        Log.d(TAG, "AlarmService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

