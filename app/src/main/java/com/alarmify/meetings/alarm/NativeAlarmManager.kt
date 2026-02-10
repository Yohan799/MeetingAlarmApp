package com.alarmify.meetings.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.alarmify.meetings.data.model.CalendarEvent

class NativeAlarmManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    companion object {
        private const val TAG = "NativeAlarmManager"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_EVENT_START_TIME = "event_start_time"
        const val EXTRA_ALARM_TIME = "alarm_time"
        const val EXTRA_MEETING_LINK = "meeting_link"
    }

    /**
     * Set an exact alarm for a calendar event
     * This uses AlarmManager.setAlarmClock() for exact timing
     */
    fun setAlarm(event: CalendarEvent, minutesBefore: Int) {
        val alarmTime = event.startTime - (minutesBefore * 60 * 1000)
        
        // Don't set alarm if time has passed
        if (alarmTime <= System.currentTimeMillis()) {
            Log.w(TAG, "Alarm time has already passed for event: ${event.title}")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.alarmify.meetings.ALARM_TRIGGER"
            putExtra(EXTRA_EVENT_ID, event.id)
            putExtra(EXTRA_EVENT_TITLE, event.title)
            putExtra(EXTRA_EVENT_START_TIME, event.startTime)
            putExtra(EXTRA_ALARM_TIME, alarmTime)
            putExtra(EXTRA_MEETING_LINK, event.meetingLink)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(), // Use event ID hash as request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create AlarmClockInfo for showing in system UI
        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            alarmTime,
            pendingIntent
        )

        try {
            // Use setAlarmClock for exact alarm that shows in status bar
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Alarm set for ${event.title} at $alarmTime (${minutesBefore} min before)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to set alarm: ${e.message}")
            // Fallback to setExactAndAllowWhileIdle if setAlarmClock fails
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Fallback alarm set for ${event.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set fallback alarm: ${e.message}")
            }
        }
    }

    /**
     * Cancel an existing alarm for an event
     */
    fun cancelAlarm(eventId: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.alarmify.meetings.ALARM_TRIGGER"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Alarm cancelled for event: $eventId")
    }

    /**
     * Check if app can schedule exact alarms
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Reschedule all alarms (useful after device reboot)
     */
    fun rescheduleAllAlarms(events: List<CalendarEvent>) {
        events.filter { it.isAlarmSet }.forEach { event ->
            setAlarm(event, event.alarmMinutesBefore)
        }
        Log.d(TAG, "Rescheduled ${events.filter { it.isAlarmSet }.size} alarms")
    }
}
