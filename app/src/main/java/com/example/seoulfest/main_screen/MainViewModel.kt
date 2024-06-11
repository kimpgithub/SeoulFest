package com.example.seoulfest.main_screen

import android.content.Context
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
    private val _notificationsEnabled = MutableStateFlow(true) // 기본값 true
    val notificationsEnabled: StateFlow<Boolean> get() = _notificationsEnabled

    fun fetchEvents(apiKey: String, today: String, selectedDistricts: List<String>) {
        viewModelScope.launch {
            try {
                val events = loadEvents(apiKey, today)
                val filteredEvents = filterAndSortEvents(events, today, selectedDistricts)
                _events.value = filteredEvents.take(10)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching events: ${e.message}")
            }
        }
    }

    private suspend fun loadEvents(apiKey: String, today: String): List<CulturalEvent> {
        val apiService = SeoulCulturalEventService.create()
        val response = apiService.getEvents(
            apiKey = apiKey,
            type = "xml",
            service = "culturalEventInfo",
            startIndex = 1,
            endIndex = 500,
            date = today
        )
        return response.events ?: emptyList()
    }

    private fun filterAndSortEvents(
        events: List<CulturalEvent>,
        today: String,
        selectedDistricts: List<String>
    ): List<CulturalEvent> {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(today)
        return events.filter { event ->
            val eventDateRange = event.date?.split("~")
            if (eventDateRange != null && eventDateRange.size == 2) {
                val startDate =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(eventDateRange[0])
                val isAfterToday = startDate?.after(todayDate) ?: false || startDate == todayDate
                val isInSelectedDistricts =
                    selectedDistricts.isEmpty() || selectedDistricts.any { district ->
                        event.guname?.contains(district) == true
                    }
                isAfterToday && isInSelectedDistricts
            } else {
                false
            }
        }.sortedBy {
            it.date?.split("~")?.get(0)
                ?.let { date ->
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                }
        }
    }

    fun loadNotificationSetting(context: Context) {
        viewModelScope.launch {
            val enabled = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .getBoolean("notifications_enabled", true)
            _notificationsEnabled.value = enabled
            Log.d("MainViewModel", "Loaded notificationsEnabled: $enabled")
        }
    }

    fun saveNotificationSetting(context: Context, isEnabled: Boolean) {
        viewModelScope.launch {
            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .edit().putBoolean("notifications_enabled", isEnabled).apply()
            _notificationsEnabled.value = isEnabled
            Log.d("MainViewModel", "Saved notificationsEnabled: $isEnabled")
        }
    }
}

