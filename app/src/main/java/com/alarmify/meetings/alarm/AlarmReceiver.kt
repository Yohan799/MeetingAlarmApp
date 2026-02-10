package com.alarmify.meetings.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received: ${intent.action}")

        if (intent.action == "com.alarmify.meetings.ALARM_TRIGGER") {
            val eventId = intent.getStringExtra(NativeAlarmManager.EXTRA_EVENT_ID) ?: return
            val eventTitle = intent.getStringExtra(NativeAlarmManager.EXTRA_EVENT_TITLE) ?: "Meeting"
            val startTime = intent.getLongExtra(NativeAlarmManager.EXTRA_EVENT_START_TIME, 0L)
            val alarmTime = intent.getLongExtra(NativeAlarmManager.EXTRA_ALARM_TIME, 0L)
            val meetingLink = intent.getStringExtra(NativeAlarmManager.EXTRA_MEETING_LINK)

            Log.d(TAG, "Triggering alarm for event: $eventTitle")

            // Start the alarm service to handle the alarm
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra(NativeAlarmManager.EXTRA_EVENT_ID, eventId)
                putExtra(NativeAlarmManager.EXTRA_EVENT_TITLE, eventTitle)
                putExtra(NativeAlarmManager.EXTRA_EVENT_START_TIME, startTime)
                putExtra(NativeAlarmManager.EXTRA_ALARM_TIME, alarmTime)
                putExtra(NativeAlarmManager.EXTRA_MEETING_LINK, meetingLink)
            }

            context.startForegroundService(serviceIntent)
        }
    }
}

