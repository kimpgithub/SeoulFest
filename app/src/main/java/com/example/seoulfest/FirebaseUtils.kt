package com.example.seoulfest.utils

import android.content.Context
import android.util.Log
import com.example.seoulfest.models.CulturalEvent
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object FirebaseUtils {

    fun initializeFirebase(context: Context) {
        FirebaseApp.initializeApp(context)
    }

    fun fetchFavorites(onResult: (List<CulturalEvent>, Int) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("favorites").document(user.uid)
                .collection("events")
                .get()
                .addOnSuccessListener { documents ->
                    val events = documents.mapNotNull { document ->
                        document.toObject(CulturalEvent::class.java).apply {
                            id = document.id
                        }
                    }
                    val upcomingEventCount = calculateUpcomingEventCount(events)
                    onResult(events, upcomingEventCount)
                }
                .addOnFailureListener { exception ->
                    Log.w("FirebaseUtils", "Error getting documents: ", exception)
                    onResult(emptyList(), 0)
                }
        } ?: onResult(emptyList(), 0)
    }

    fun calculateUpcomingEventCount(events: List<CulturalEvent>): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val next30Days = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 30)
        }.time

        return events.count { event ->
            val eventDateRange = event.date?.split("~")
            if (eventDateRange != null && eventDateRange.size == 2) {
                val startDate =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(eventDateRange[0])
                (startDate?.after(today) == true || startDate == today) && startDate.before(
                    next30Days
                )
            } else {
                false
            }
        }
    }
}