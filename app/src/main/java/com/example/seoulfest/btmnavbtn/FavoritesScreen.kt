package com.example.seoulfest.btmnavbtn

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.seoulfest.models.CulturalEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavHostController) {
    val events = remember { mutableStateOf<List<CulturalEvent>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        fetchFavorites(
            onSuccess = { fetchedEvents ->
                events.value = fetchedEvents
            },
            onFailure = { exception ->
                Toast.makeText(context, "Failed to load favorites: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.w("Firestore", "Error getting documents: ", exception)
            }
        )
    }

    fun deleteFavorite(eventId: String) {
        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser

        user?.let { it ->
            db.collection("favorites").document(it.uid)
                .collection("events").document(eventId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Favorite deleted!", Toast.LENGTH_SHORT).show()
                    events.value = events.value.filterNot { it.id == eventId }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to delete favorite.", Toast.LENGTH_SHORT).show()
                    Log.w("Firestore", "Error deleting favorite event", e)
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(events.value) { event ->
                EventItem(
                    title = event.title ?: "",
                    date = event.date ?: "",
                    location = event.place ?: "",
                    pay = event.useFee ?: "",
                    imageUrl = event.mainImg ?: "",
                    navController = navController,
                    onDelete = { deleteFavorite(event.id) }
                )
            }
        }
    }
}

fun fetchFavorites(onSuccess: (List<CulturalEvent>) -> Unit, onFailure: (Exception) -> Unit) {
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
                onSuccess(events)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}

@Composable
fun EventItem(
    title: String,
    date: String,
    location: String,
    pay: String,
    imageUrl: String,
    navController: NavHostController,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable {
                val encodedTitle = URLEncoder.encode(title, "UTF-8")
                val encodedDate = URLEncoder.encode(date, "UTF-8")
                val encodedLocation = URLEncoder.encode(location, "UTF-8")
                val encodedPay = URLEncoder.encode(pay, "UTF-8")
                val encodedImageUrl = URLEncoder.encode(imageUrl, "UTF-8")

                navController.navigate(
                    "detail?title=$encodedTitle&date=$encodedDate&location=$encodedLocation&pay=$encodedPay&imageUrl=$encodedImageUrl"
                )
            }
    ) {
        Row {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = date, style = MaterialTheme.typography.bodySmall)
                Text(text = location, style = MaterialTheme.typography.bodySmall)
                Text(text = pay, style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
