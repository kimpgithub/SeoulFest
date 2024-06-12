package com.example.seoulfest.main_screen

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seoulfest.models.CulturalEvent
import com.example.seoulfest.network.SeoulCulturalEventService
import com.example.seoulfest.utils.FirebaseUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class MainViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<CulturalEvent>>(emptyList())
    val events: StateFlow<List<CulturalEvent>> get() = _events
    private val _upcomingEventCount = MutableStateFlow(0)
    val upcomingEventCount: StateFlow<Int> get() = _upcomingEventCount
    private val _notificationsEnabled = MutableStateFlow(true) // 기본값 true
    val notificationsEnabled: StateFlow<Boolean> get() = _notificationsEnabled

    fun fetchEvents(apiKey: String, selectedStartDate: String, selectedEndDate: String, selectedDistricts: List<String>) {
        viewModelScope.launch {
            try {
                val events = loadEvents(apiKey, selectedStartDate, selectedEndDate)
                val filteredEvents = filterAndSortEvents(events, selectedStartDate, selectedEndDate, selectedDistricts)
                _events.value = filteredEvents.take(10)

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching events: ${e.message}")
            }
        }
    }

    fun fetchFavoritesAndUpdateCount() {
        viewModelScope.launch {
            FirebaseUtils.fetchFavorites { events, count ->
                _events.value = events
                _upcomingEventCount.value = count
            }
        }
    }

    private suspend fun loadEvents(apiKey: String, selectedStartDate: String, selectedEndDate: String): List<CulturalEvent> {
        val apiService = SeoulCulturalEventService.create()
        val response = apiService.getEvents(
            apiKey = apiKey,
            type = "xml",
            service = "culturalEventInfo",
            startIndex = 1,
            endIndex = 500,
            date = "$selectedStartDate~$selectedEndDate"
        )
        return response.events ?: emptyList()
    }


    private fun filterAndSortEvents(
        events: List<CulturalEvent>,
        selectedStartDate: String,
        selectedEndDate: String,
        selectedDistricts: List<String>
    ): List<CulturalEvent> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDateParsed = try {
            dateFormat.parse(selectedStartDate)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error parsing selectedStartDate: ${e.message}")
            Date() // 기본값으로 현재 날짜를 사용
        }
        val endDateParsed = try {
            dateFormat.parse(selectedEndDate)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error parsing selectedEndDate: ${e.message}")
            Date() // 기본값으로 현재 날짜를 사용
        }

        return events.filter { event ->
            val eventDateRange = event.date?.split("~")
            if (eventDateRange != null && eventDateRange.size == 2) {
                val eventStartDate = try {
                    dateFormat.parse(eventDateRange[0])
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error parsing event start date: ${e.message}")
                    null
                }
                val eventEndDate = try {
                    dateFormat.parse(eventDateRange[1])
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error parsing event end date: ${e.message}")
                    null
                }
                val isInDateRange = (eventStartDate?.after(startDateParsed) ?: false || eventStartDate == startDateParsed) &&
                        (eventEndDate?.before(endDateParsed) ?: false || eventEndDate == endDateParsed)
                val isInSelectedDistricts = selectedDistricts.isEmpty() || selectedDistricts.any { district ->
                    event.guname?.contains(district) == true
                }
                isInDateRange && isInSelectedDistricts
            } else {
                false
            }
        }.sortedBy {
            it.date?.split("~")?.get(0)
                ?.let { date ->
                    try {
                        dateFormat.parse(date)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error parsing event sort date: ${e.message}")
                        null
                    }
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

