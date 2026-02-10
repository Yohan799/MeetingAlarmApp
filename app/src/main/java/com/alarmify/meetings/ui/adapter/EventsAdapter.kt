package com.alarmify.meetings.ui.adapter

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
    private val onCancelAlarmClick: (CalendarEvent) -> Unit
) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    inner class EventViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: CalendarEvent) {
            binding.tvEventTitle.text = event.title
            binding.tvEventTime.text = event.getFormattedStartTime()

            if (event.isAlarmSet) {
                binding.btnSetAlarm.text = "Alarm: ${event.alarmMinutesBefore} min"
                binding.btnSetAlarm.setBackgroundResource(R.drawable.gradient_button_modern)
                binding.btnCancelAlarm.visibility = View.VISIBLE
                binding.spacer1.visibility = View.VISIBLE
                binding.alarmStatusBadge.visibility = View.VISIBLE
                binding.alarmIndicator.visibility = View.VISIBLE
            } else {
                binding.btnSetAlarm.text = "Set Alarm"
                binding.btnSetAlarm.setBackgroundResource(R.drawable.gradient_button_modern)
                binding.btnCancelAlarm.visibility = View.GONE
                binding.spacer1.visibility = View.GONE
                binding.alarmStatusBadge.visibility = View.GONE
                binding.alarmIndicator.visibility = View.GONE
            }

            // Show Join button if meeting link is available
            if (!event.meetingLink.isNullOrBlank()) {
                binding.btnJoinMeeting.visibility = View.VISIBLE
                binding.spacer2.visibility = View.VISIBLE
                binding.btnJoinMeeting.setOnClickListener {
                    openMeetingLink(event.meetingLink)
                }
            } else {
                binding.btnJoinMeeting.visibility = View.GONE
                binding.spacer2.visibility = View.GONE
            }

            binding.btnSetAlarm.setOnClickListener {
                onAlarmClick(event)
            }

            binding.btnCancelAlarm.setOnClickListener {
                onCancelAlarmClick(event)
            }
        }
        
        private fun openMeetingLink(link: String?) {
            link?.let {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                    binding.root.context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle error opening link
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
