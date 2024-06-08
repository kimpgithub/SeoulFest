package com.example.seoulfest.detailscreen

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import java.net.URLDecoder
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    title: String,
    date: String,
    location: String,
    pay: String,
    imageUrl: String,
    navController: NavHostController
) {
    val decodedTitle = try {
        URLDecoder.decode(title, "UTF-8")
    } catch (e: IllegalArgumentException) {
        title // Fallback to original if decoding fails
    }

    val decodedDate = try {
        URLDecoder.decode(date, "UTF-8")
    } catch (e: IllegalArgumentException) {
        date
    }

    val decodedLocation = try {
        URLDecoder.decode(location, "UTF-8")
    } catch (e: IllegalArgumentException) {
        location
    }

    val decodedPay = try {
        URLDecoder.decode(pay, "UTF-8")
    } catch (e: IllegalArgumentException) {
        pay
    }

    val decodedImageUrl = try {
        URLDecoder.decode(imageUrl, "UTF-8")
    } catch (e: IllegalArgumentException) {
        imageUrl
    }

    val context = LocalContext.current
    val isFavorite = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        checkIfFavorite(decodedTitle, decodedDate, decodedLocation) { exists ->
            isFavorite.value = exists
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("Detail") },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = {
                    if (isFavorite.value) {
                        removeFavorite(decodedTitle, decodedDate, decodedLocation, context) {
                            isFavorite.value = false
                        }
                    } else {
                        saveFavorite(decodedTitle, decodedDate, decodedLocation, decodedPay, decodedImageUrl, context) {
                            isFavorite.value = true
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isFavorite.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite.value) "Unfavorite" else "Favorite"
                    )
                }
            }
        )
        AsyncImage(
            model = decodedImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth()
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(decodedTitle, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(decodedDate, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(decodedLocation, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(decodedPay, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
fun saveFavorite(title: String, date: String, location: String, pay: String, imageUrl: String, context: Context, onSuccess: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    if (user != null) {
        val favoriteEvent = hashMapOf(
            "title" to title,
            "date" to date,
            "place" to location,  // Correct field name
            "useFee" to pay,      // Correct field name
            "mainImg" to imageUrl // Correct field name
        )

        db.collection("favorites").document(user.uid)
            .collection("events")
            .whereEqualTo("title", title)
            .whereEqualTo("date", date)
            .whereEqualTo("place", location)  // Correct field name
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    db.collection("favorites").document(user.uid)
                        .collection("events")
                        .add(favoriteEvent)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Favorite added!", Toast.LENGTH_SHORT).show()
                            Log.d("Firestore", "Favorite event successfully added!")
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to add favorite.", Toast.LENGTH_SHORT).show()
                            Log.w("Firestore", "Error adding favorite event", e)
                        }
                } else {
                    Toast.makeText(context, "This event is already in your favorites.", Toast.LENGTH_SHORT).show()
                    Log.d("Firestore", "This event is already in your favorites.")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to check favorites.", Toast.LENGTH_SHORT).show()
                Log.w("Firestore", "Error checking favorites", e)
            }
    } else {
        Toast.makeText(context, "No authenticated user found.", Toast.LENGTH_SHORT).show()
        Log.w("Firestore", "No authenticated user found.")
    }
}

fun checkIfFavorite(title: String, date: String, location: String, callback: (Boolean) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    if (user != null) {
        db.collection("favorites").document(user.uid)
            .collection("events")
            .whereEqualTo("title", title)
            .whereEqualTo("date", date)
            .whereEqualTo("place", location)  // Correct field name
            .get()
            .addOnSuccessListener { documents ->
                callback(!documents.isEmpty)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error checking if event is favorite", e)
                callback(false)
            }
    } else {
        callback(false)
    }
}
fun removeFavorite(title: String, date: String, location: String, context: Context, onSuccess: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    if (user != null) {
        db.collection("favorites").document(user.uid)
            .collection("events")
            .whereEqualTo("title", title)
            .whereEqualTo("date", date)
            .whereEqualTo("place", location)  // Correct field name
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection("favorites").document(user.uid)
                        .collection("events")
                        .document(document.id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Favorite removed!", Toast.LENGTH_SHORT).show()
                            Log.d("Firestore", "Favorite event successfully removed!")
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to remove favorite.", Toast.LENGTH_SHORT).show()
                            Log.w("Firestore", "Error removing favorite event", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to check favorites.", Toast.LENGTH_SHORT).show()
                Log.w("Firestore", "Error checking favorites", e)
            }
    } else {
        Toast.makeText(context, "No authenticated user found.", Toast.LENGTH_SHORT).show()
        Log.w("Firestore", "No authenticated user found.")
    }
}