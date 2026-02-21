package com.alarmify.meetings.data.model.fathom

import com.google.gson.annotations.SerializedName

data class FathomMeetingListResponse(
    @SerializedName("items") val items: List<FathomMeeting>,
    @SerializedName("next_cursor") val nextCursor: String?
)

data class FathomMeeting(
    @SerializedName("id") val id: String,
    @SerializedName("meeting_title") val title: String?, // Sometimes title is in 'meeting_title'
    @SerializedName("title") val originalTitle: String?, // Or just 'title'
    @SerializedName("scheduled_start_time") val startTime: String?,
    @SerializedName("recording_start_time") val recordingStartTime: String?,
    @SerializedName("share_url") val shareUrl: String?
) {
    // Helper to get the best available title
    val displayTitle: String
        get() = title ?: originalTitle ?: "Untitled Meeting"
        
    // Helper to get the best available time
    val eventTime: String?
        get() = startTime ?: recordingStartTime
}

data class FathomSummaryResponse(
    @SerializedName("markdown_formatted") val summary: String?,
    @SerializedName("action_items") val actionItems: List<FathomActionItem>?
)

data class FathomActionItem(
    @SerializedName("description") val description: String,
    @SerializedName("assignee") val assignee: FathomAssignee?
)

data class FathomAssignee(
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?
)
