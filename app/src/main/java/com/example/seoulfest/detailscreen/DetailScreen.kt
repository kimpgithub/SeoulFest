package com.example.seoulfest.detailscreen

import android.util.Log
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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
                    saveFavorite(title, date, location, pay, imageUrl)
                }){
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite")
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
fun saveFavorite(title: String, date: String, location: String, pay: String, imageUrl: String) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    if (user != null) {
        val favoriteEvent = hashMapOf(
            "title" to title,
            "date" to date,
            "location" to location,
            "pay" to pay,
            "imageUrl" to imageUrl
        )

        db.collection("favorites").document(user.uid)
            .collection("events")
            .add(favoriteEvent)
            .addOnSuccessListener {
                Log.d("Firestore", "Favorite event successfully added!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding favorite event", e)
            }
    } else {
        Log.w("Firestore", "No authenticated user found.")
    }
}
