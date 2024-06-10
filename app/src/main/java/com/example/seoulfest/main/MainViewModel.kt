package com.example.seoulfest.main

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
                val apiService = SeoulCulturalEventService.create()
                val response = apiService.getEvents(
                    apiKey = apiKey,
                    type = "xml",
                    service = "culturalEventInfo",
                    startIndex = 1,
                    endIndex = 500, // 더 많은 이벤트를 가져오기 위해 endIndex를 늘림
                    date = today
                )
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(today)

                // 이벤트 필터링 및 정렬
                val filteredSortedEvents = response.events
                    ?.filter { event ->
                        val eventDateRange = event.date?.split("~")
                        if (eventDateRange != null && eventDateRange.size == 2) {
                            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(eventDateRange[0])
                            val isAfterToday = startDate?.after(todayDate) ?: false || startDate == todayDate
                            val isInSelectedDistricts = selectedDistricts.isEmpty() || selectedDistricts.any { district ->
                                event.guname?.contains(district) == true
                            }
                            isAfterToday && isInSelectedDistricts
                        } else {
                            false
                        }
                    }
                    ?.sortedBy {
                        it.date?.split("~")?.get(0)
                            ?.let { date ->
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                            }
                    }

                // 필터링된 이벤트를 StateFlow에 설정
                _events.value = filteredSortedEvents?.take(10) ?: emptyList()
            } catch (e: Exception) {
                // 에러 로그 출력
                e.printStackTrace()
            }
        }
    }

    fun loadNotificationSetting(context: Context) {
        viewModelScope.launch {
            Log.d("MainViewModel", "Attempting to load notification settings")
            val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val enabled = sharedPreferences.getBoolean("notifications_enabled", true)
            Log.d("MainViewModel", "Loaded notificationsEnabled: $enabled")
            _notificationsEnabled.value = enabled
        }
    }

    fun saveNotificationSetting(context: Context, isEnabled: Boolean) {
        viewModelScope.launch {
            val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            sharedPreferences.edit().putBoolean("notifications_enabled", isEnabled).apply()
            Log.d("MainViewModel", "Saved notificationsEnabled: $isEnabled")
            _notificationsEnabled.value = isEnabled
        }
    }
}
