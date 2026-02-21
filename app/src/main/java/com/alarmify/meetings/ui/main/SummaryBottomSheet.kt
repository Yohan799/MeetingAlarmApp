package com.alarmify.meetings.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alarmify.meetings.data.model.CalendarEvent
import com.alarmify.meetings.data.model.fathom.FathomSummaryResponse
import com.alarmify.meetings.databinding.BottomSheetSummaryBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SummaryBottomSheet(
    private val event: CalendarEvent,
    private val summaryResponse: FathomSummaryResponse
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetSummaryBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tvTitle.text = event.title
        binding.tvDate.text = event.getFormattedStartTime()
        
        // Populate Summary
        binding.tvSummaryContent.text = summaryResponse.summary ?: "No summary available."
        
        // Populate Action Items
        val actionItemsText = StringBuilder()
        summaryResponse.actionItems?.forEach { item ->
            val assignee = item.assignee?.name ?: "Unassigned"
            actionItemsText.append("• ${item.description} ($assignee)\n")
        }
        
        if (actionItemsText.isNotEmpty()) {
            binding.tvActionItems.text = actionItemsText.toString().trim()
        } else {
            binding.tvActionItems.text = "No action items recorded."
        }
    }
}
