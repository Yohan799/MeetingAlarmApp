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
     * Fetch upcoming events from ALL authorized Google Accounts (next 30 days)
     */
    suspend fun fetchUpcomingEvents(): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val allEvents = mutableListOf<CalendarEvent>()
        val accountRepository = AccountRepository(context)
        val accounts = accountRepository.getAuthorizedAccounts()

        if (accounts.isEmpty()) {
            // Fallback: Check for legacy single account if no multi-accounts stored yet
            val legacyAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (legacyAccount != null) {
                // Migrate legacy account to new repository
                accountRepository.addAccount(legacyAccount.email ?: "")
                return@withContext fetchEventsForAccount(legacyAccount.email ?: "")
            }
            return@withContext emptyList()
        }

        for (email in accounts) {
            try {
                val accountEvents = fetchEventsForAccount(email)
                allEvents.addAll(accountEvents)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching events for $email", e)
            }
        }

        // Sort all merged events by start time
        allEvents.sortBy { it.startTime }

        return@withContext allEvents
    }

    /**
     * Helper to fetch events for a single account email
     */
    private fun fetchEventsForAccount(email: String): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        
        try {
            // Create credential for this specific account
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(CalendarScopes.CALENDAR_READONLY)
            ).apply {
                selectedAccountName = email
            }

            val service = Calendar.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(APPLICATION_NAME)
                .build()

            val now = DateTime(System.currentTimeMillis())
            val thirtyDaysLater = DateTime(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000))

            // Get all calendars for this account
            val calendarList = service.calendarList().list().execute()
            val calendars = calendarList.items ?: emptyList()
            
            val calendarIds = calendars.map { it.id }

            for (calendarId in calendarIds) {
                try {
                    val eventList = service.events().list(calendarId)
                        .setTimeMin(now)
                        .setTimeMax(thirtyDaysLater)
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .setMaxResults(100)
                        .execute()

                    val calendarEvents = eventList.items ?: emptyList()
                    
                    calendarEvents.forEach { event ->
                        val startTime = event.start?.dateTime?.value 
                            ?: event.start?.date?.value 
                            ?: 0L
                        
                        val endTime = event.end?.dateTime?.value 
                            ?: event.end?.date?.value 
                            ?: 0L

                        val isAllDay = event.start?.date != null && event.start?.dateTime == null
                        val meetingLink = extractMeetingLink(event)

                        events.add(
                            CalendarEvent(
                                id = event.id ?: "",
                                title = event.summary ?: "Untitled Event",
                                description = event.description,
                                startTime = startTime,
                                endTime = endTime,
                                location = event.location,
                                meetingLink = meetingLink,
                                isAllDay = isAllDay,
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching calendar $calendarId", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching events for account $email", e)
            throw e
        }
        
        return events
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
