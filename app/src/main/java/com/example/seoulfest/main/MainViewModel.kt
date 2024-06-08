package com.example.seoulfest.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seoulfest.models.CulturalEvent
import com.example.seoulfest.network.SeoulCulturalEventService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<CulturalEvent>>(emptyList())
    val events: StateFlow<List<CulturalEvent>> get() = _events

    fun fetchEvents(apiKey: String, today: String, selectedDistricts: List<String>) {
        viewModelScope.launch {
            try {
                val apiService = SeoulCulturalEventService.create()
                val response = apiService.getEvents(
                    apiKey = apiKey,
                    type = "xml",
                    service = "culturalEventInfo",
                    startIndex = 1,
                    endIndex = 100,
                    date = today
                )
                val filteredEvents = response.events?.filter { event ->
                    selectedDistricts.isEmpty() || selectedDistricts.any { district ->
                        event.guname?.contains(district) == true
                    }
                }
                val sortedEvents = filteredEvents?.sortedBy {
                    it.date?.split("~")?.get(0)
                        ?.let { date ->
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                        }
                }
                _events.value = sortedEvents?.take(10) ?: emptyList()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to fetch events", e)
            }
        }
    }
}
