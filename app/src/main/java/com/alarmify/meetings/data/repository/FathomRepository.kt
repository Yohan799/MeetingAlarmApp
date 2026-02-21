package com.alarmify.meetings.data.repository

import android.util.Log
import com.alarmify.meetings.data.auth.FathomAuthManager
import com.alarmify.meetings.data.model.fathom.FathomMeeting
import com.alarmify.meetings.data.model.fathom.FathomMeetingListResponse
import com.alarmify.meetings.data.model.fathom.FathomSummaryResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class FathomRepository(private val authManager: FathomAuthManager) {

    private val client = OkHttpClient()
    private val gson = Gson()
    
    companion object {
        private const val BASE_URL = "https://api.fathom.ai/external/v1"
        private const val TAG = "FathomRepository"
    }

    suspend fun getRecentMeetings(): List<FathomMeeting> = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken() ?: return@withContext emptyList()
        
        val request = Request.Builder()
            .url("$BASE_URL/meetings?limit=20") // Fetch last 20 meetings
            .header("Authorization", "Bearer $token")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch meetings: ${response.code}")
                    return@withContext emptyList()
                }

                val body = response.body?.string() ?: return@withContext emptyList()
                val listResponse = gson.fromJson(body, FathomMeetingListResponse::class.java)
                return@withContext listResponse.items
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching meetings", e)
            return@withContext emptyList()
        }
    }

    // For OAuth apps, summaries/action items might not be in the list-meetings response
    // So we fetch specific recording details if needed. 
    // However, the /recordings/{id} endpoint structure is similar
    suspend fun getMeetingSummary(recordingId: String): FathomSummaryResponse? = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken() ?: return@withContext null
        
        // Note: Docs say /recordings/{id} or /recordings/{id}/summary?
        // Let's try /recordings/{id} first as it usually contains everything
        val request = Request.Builder()
            .url("$BASE_URL/recordings/$recordingId")
            .header("Authorization", "Bearer $token")
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                 if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch summary: ${response.code}")
                    return@withContext null
                }
                
                val body = response.body?.string() ?: return@withContext null
                // Reuse FathomMeeting parsing or specific summary parsing?
                // The structure for recording details usually includes 'default_summary' and 'action_items'
                // Let's reuse FathomSummaryResponse to parse just those fields
                /* 
                   Response shape typicaly:
                   {
                     "id": "...",
                     "default_summary": { ... },
                     "action_items": [ ... ]
                   }
                */
                // We create a wrapper class locally or just parse loosely
                // For simplicity, let's assume the body structure matches what we need
                // We might need a specific wrapper if the fields are nested under "default_summary"
                
                // Let's try fetching the summary endpoint specifically if it exists, 
                // but list-meetings docs said "use /recordings". 
                // Let's assume /recordings/{id} returns the full object.
                
                // We need to parse nested objects. Let's do a trick and parse the whole thing 
                // into a Map or specific class?
                // Let's parse into FathomMeeting (which doesn't have summary fields yet)
                // Let's add those fields to FathomMeeting or fetch separately.
                
                // Better approach: Parse strictly for UI needs
                val jsonObject = com.google.gson.JsonParser.parseString(body).asJsonObject
                
                val summaryObj = if (jsonObject.has("default_summary")) jsonObject.getAsJsonObject("default_summary") else null
                val markdown = summaryObj?.get("markdown_formatted")?.asString
                
                val actionItemsArr = if (jsonObject.has("action_items")) jsonObject.getAsJsonArray("action_items") else null
                val actionItems = if (actionItemsArr != null) {
                    gson.fromJson(actionItemsArr, Array<com.alarmify.meetings.data.model.fathom.FathomActionItem>::class.java).toList()
                } else emptyList()
                
                return@withContext FathomSummaryResponse(markdown, actionItems)
            }
        } catch (e: Exception) {
             Log.e(TAG, "Error fetching summary", e)
             return@withContext null
        }
    }
}
