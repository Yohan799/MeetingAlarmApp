package com.alarmify.meetings.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.alarmify.meetings.data.model.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for fetching events from Google Calendar API
 */
class CalendarRepository(private val context: Context) {

    companion object {
        private const val TAG = "CalendarRepository"
        private const val APPLICATION_NAME = "Meeting Alarm App"
    }

    /**
     * Get Google Calendar service using the signed-in account
     */
    private fun getCalendarService(): Calendar? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.e(TAG, "No signed-in account found")
            return null
        }

        Log.d(TAG, "Using account: ${account.email}")

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(CalendarScopes.CALENDAR_READONLY)
        ).apply {
            selectedAccount = account.account
        }

        return Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    /**
     * Fetch upcoming events from ALL Google Calendars (next 30 days)
     */
    suspend fun fetchUpcomingEvents(): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val events = mutableListOf<CalendarEvent>()

        try {
            val service = getCalendarService()
            if (service == null) {
                Log.e(TAG, "Calendar service is null - user not signed in?")
                return@withContext events
            }

            val now = DateTime(System.currentTimeMillis())
            val thirtyDaysLater = DateTime(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000))

            Log.d(TAG, "========== GOOGLE CALENDAR DEBUG ==========")
            Log.d(TAG, "Fetching events from: $now to $thirtyDaysLater")

            // Get all calendars the user has access to
            val calendarList = service.calendarList().list().execute()
            val calendars = calendarList.items ?: emptyList()
            
            Log.d(TAG, "Found ${calendars.size} calendars:")
            calendars.forEachIndexed { index, cal ->
                Log.d(TAG, "  ${index + 1}. ${cal.summary} (ID: ${cal.id})")
            }
            
            val calendarIds = calendars.map { it.id }

            // Fetch events from each calendar
            for (calendarId in calendarIds) {
                try {
                    val eventList = service.events().list(calendarId)
                        .setTimeMin(now)
                        .setTimeMax(thirtyDaysLater)
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .setMaxResults(250)
                        .execute()

                    val calendarEvents = eventList.items ?: emptyList()
                    Log.d(TAG, "Calendar '${calendars.find { it.id == calendarId }?.summary ?: calendarId}': ${calendarEvents.size} events")
                    
                    calendarEvents.forEach { event ->
                        Log.d(TAG, "  - ${event.summary} | ${event.start?.dateTime ?: event.start?.date}")
                        
                        // Get start time (handle both dateTime and date-only events)
                        val startTime = event.start?.dateTime?.value 
                            ?: event.start?.date?.value 
                            ?: 0L
                        
                        // Get end time
                        val endTime = event.end?.dateTime?.value 
                            ?: event.end?.date?.value 
                            ?: 0L

                        // Determine if it's an all-day event
                        val isAllDay = event.start?.date != null && event.start?.dateTime == null

                        // Extract meeting link from various sources
                        val meetingLink = extractMeetingLink(event)
                        
                        if (meetingLink != null) {
                            Log.d(TAG, "    Meeting link found: $meetingLink")
                        }

                        events.add(
                            CalendarEvent(
                                id = event.id ?: "",
                                title = event.summary ?: "Untitled Event",
                                description = event.description,
                                startTime = startTime,
                                endTime = endTime,
                                location = event.location,
                                meetingLink = meetingLink,
                                isAllDay = isAllDay
                            )
                        )
                    }
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Could not fetch events from calendar $calendarId: ${e.message}")
                }
            }

            // Sort all events by start time
            events.sortBy { it.startTime }
            
            Log.d(TAG, "========== SUMMARY ==========")
            Log.d(TAG, "Total events from all calendars: ${events.size}")
            Log.d(TAG, "==============================")

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching events: ${e.message}", e)
        }

        return@withContext events
    }
    
    /**
     * Extract meeting link from various sources in the event
     * Priority: hangoutLink > conferenceData > location > description
     */
    private fun extractMeetingLink(event: com.google.api.services.calendar.model.Event): String? {
        // 1. Check hangoutLink (direct Google Meet link)
        event.hangoutLink?.let { 
            if (it.isNotBlank()) return it 
        }
        
        // 2. Check conferenceData for video conference entry points
        event.conferenceData?.entryPoints?.forEach { entryPoint ->
            if (entryPoint.entryPointType == "video") {
                entryPoint.uri?.let { if (it.isNotBlank()) return it }
            }
        }
        
        // 3. Check location for meeting URLs
        event.location?.let { location ->
            findMeetingUrl(location)?.let { return it }
        }
        
        // 4. Check description for meeting URLs
        event.description?.let { description ->
            findMeetingUrl(description)?.let { return it }
        }
        
        return null
    }
    
    /**
     * Find meeting URL in text (supports Google Meet, Zoom, Teams, Webex)
     */
    private fun findMeetingUrl(text: String): String? {
        val meetingPatterns = listOf(
            "https://meet\\.google\\.com/[a-zA-Z0-9\\-]+",
            "https://[a-zA-Z0-9]+\\.zoom\\.us/j/[0-9]+[^\\s]*",
            "https://teams\\.microsoft\\.com/l/meetup-join/[^\\s]+",
            "https://[a-zA-Z0-9]+\\.webex\\.com/[^\\s]+"
        )
        
        for (pattern in meetingPatterns) {
            val regex = Regex(pattern)
            regex.find(text)?.let { matchResult ->
                return matchResult.value
            }
        }
        
        return null
    }
}
