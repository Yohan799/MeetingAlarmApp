package com.alarmify.meetings.ui.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.alarmify.meetings.R
import com.alarmify.meetings.data.model.CalendarEvent
import com.alarmify.meetings.databinding.ItemEventBinding

class EventsAdapter(
    private val events: List<CalendarEvent>,
    private val onAlarmClick: (CalendarEvent) -> Unit,
    private val onCancelAlarmClick: (CalendarEvent) -> Unit,
    private val onViewNotesClick: (CalendarEvent) -> Unit
) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    inner class EventViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: CalendarEvent) {
            binding.tvEventTitle.text = event.title
            binding.tvEventTime.text = event.getFormattedStartTime()

            // Fathom Notes Logic
            if (event.fathomRecordingId != null) {
                binding.btnViewNotes.visibility = View.VISIBLE
                binding.btnViewNotes.setOnClickListener {
                    onViewNotesClick(event)
                }
            } else {
                binding.btnViewNotes.visibility = View.GONE
            }

            // Alarm Status Logic
            if (event.isAlarmSet) {
                binding.chipStatus.visibility = View.VISIBLE
                binding.chipStatus.text = "Alarm: ${event.alarmMinutesBefore}m before"
                binding.chipStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.teal_core))
                
                binding.btnSetAlarm.text = "Edit"
                binding.btnCancelAlarm.visibility = View.VISIBLE
            } else {
                binding.chipStatus.visibility = View.GONE
                
                binding.btnSetAlarm.text = "Set Alarm"
                binding.btnCancelAlarm.visibility = View.GONE
            }

            // Meeting Link Logic
            if (!event.meetingLink.isNullOrBlank()) {
                binding.btnJoinMeeting.visibility = View.VISIBLE
                binding.btnJoinMeeting.setOnClickListener {
                    openMeetingLink(binding.root.context, event.meetingLink)
                }
            } else {
                binding.btnJoinMeeting.visibility = View.GONE
            }

            binding.btnSetAlarm.setOnClickListener {
                onAlarmClick(event)
            }

            binding.btnCancelAlarm.setOnClickListener {
                onCancelAlarmClick(event)
            }
        }
        
        private fun openMeetingLink(context: Context, link: String?) {
            link?.let {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle error opening link generically or log
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size
}
