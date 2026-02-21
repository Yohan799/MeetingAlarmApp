package com.alarmify.meetings.data.model

import java.util.Date

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val location: String?,
    val meetingLink: String? = null,
    val isAlarmSet: Boolean = false,
    val alarmMinutesBefore: Int = 0,
    val isAllDay: Boolean = false,
    val fathomRecordingId: String? = null
) {
    fun getFormattedStartTime(): String {
        val date = Date(startTime)
        return android.text.format.DateFormat.format("dd MMM yyyy, hh:mm a", date).toString()
    }
    
    fun getFormattedEndTime(): String {
        val date = Date(endTime)
        return android.text.format.DateFormat.format("hh:mm a", date).toString()
    }
    
    fun getAlarmTime(): Long {
        return startTime - (alarmMinutesBefore * 60 * 1000)
    }
}

