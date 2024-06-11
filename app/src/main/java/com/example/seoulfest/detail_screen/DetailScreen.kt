package com.example.seoulfest.detail_screen

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.seoulfest.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLDecoder

@Composable
fun DetailScreen(
    title: String,
    date: String,
    location: String,
    pay: String,
    imageUrl: String,
    navController: NavHostController
) {
    val decodedTitle = decodeString(title)
    val decodedDate = decodeString(date)
    val decodedLocation = decodeString(location)
    val decodedPay = decodeString(pay)
    val decodedImageUrl = decodeString(imageUrl)

    val context = LocalContext.current
    val isFavorite = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        checkIfFavorite(decodedTitle, decodedDate, decodedLocation) { exists ->
            isFavorite.value = exists
        }
    }

    DetailScreenContent(
        decodedTitle, decodedDate, decodedLocation, decodedPay, decodedImageUrl,
        navController, isFavorite.value
    ) { favorite ->
        if (favorite) {
            removeFavorite(decodedTitle, decodedDate, decodedLocation, context) {
                isFavorite.value = false
            }
        } else {
            saveFavorite(
                decodedTitle,
                decodedDate,
                decodedLocation,
                decodedPay,
                decodedImageUrl,
                context
            ) {
                isFavorite.value = true
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenContent(
    title: String, date: String, location: String, pay: String, imageUrl: String,
    navController: NavHostController, isFavorite: Boolean, onFavoriteClick: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("상세화면", color = colorResource(id = R.color.colorTextPrimary)) },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colorResource(id = R.color.colorTextPrimary)
                    )
                }
            },
            actions = {
                IconButton(onClick = { onFavoriteClick(isFavorite) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                        tint = Color.Red
                    )
                }
            },
            colors = TopAppBarDefaults.mediumTopAppBarColors(
                containerColor = colorResource(id = R.color.colorPrimary)
            )
        )
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White) // 이미지 아래 공간의 배경을 흰색으로 설정
                .padding(16.dp)
                .weight(1f, fill = true) // 화면의 남은 공간을 채움
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(color = colorResource(id = R.color.colorTextPrimary)))
            Spacer(modifier = Modifier.height(8.dp))
            Text(date, style = MaterialTheme.typography.bodyMedium.copy(color = colorResource(id = R.color.colorTextSecondary)))
            Spacer(modifier = Modifier.height(8.dp))
            Text(location, style = MaterialTheme.typography.bodyMedium.copy(color = colorResource(id = R.color.colorTextSecondary)))
            Spacer(modifier = Modifier.height(8.dp))
            Text(pay, style = MaterialTheme.typography.bodyMedium.copy(color = colorResource(id = R.color.colorTextSecondary)))
        }
    }
}

fun decodeString(value: String): String {
    return try {
        URLDecoder.decode(value, "UTF-8")
    } catch (e: IllegalArgumentException) {
        value
    }
}

fun saveFavorite(
    title: String,
    date: String,
    location: String,
    pay: String,
    imageUrl: String,
    context: Context,
    onSuccess: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    if (user != null) {
        val favoriteEvent = createFavoriteEventMap(title, date, location, pay, imageUrl)

        db.collection("favorites").document(user.uid)
            .collection("events")
            .whereEqualTo("title", title)
            .whereEqualTo("date", date)
            .whereEqualTo("place", location)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    addFavoriteToFirestore(db, user.uid, favoriteEvent, context, onSuccess)
                } else {
                    showToast(context, "This event is already in your favorites.")
                }
            }
            .addOnFailureListener { e ->
                showToast(context, "Failed to check favorites.")
                Log.w("Firestore", "Error checking favorites", e)
            }
    } else {
        showToast(context, "No authenticated user found.")
    }
}

fun createFavoriteEventMap(
    title: String,
    date: String,
    location: String,
    pay: String,
    imageUrl: String
): Map<String, String> {
    return hashMapOf(
        "title" to title,
        "date" to date,
        "place" to location,
        "useFee" to pay,
        "mainImg" to imageUrl
    )
}

fun addFavoriteToFirestore(
    db: FirebaseFirestore,
    uid: String,
    favoriteEvent: Map<String, String>,
    context: Context,
    onSuccess: () -> Unit
) {
    db.collection("favorites").document(uid)
        .collection("events")
        .add(favoriteEvent)
        .addOnSuccessListener {
            showToast(context, "Favorite added!")
            Log.d("Firestore", "Favorite event successfully added!")
            onSuccess()
        }
        .addOnFailureListener { e ->
            showToast(context, "Failed to add favorite.")
            Log.w("Firestore", "Error adding favorite event", e)
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
            .whereEqualTo("place", location)
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

fun removeFavorite(
    title: String,
    date: String,
    location: String,
    context: Context,
    onSuccess: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    if (user != null) {
        db.collection("favorites").document(user.uid)
            .collection("events")
            .whereEqualTo("title", title)
            .whereEqualTo("date", date)
            .whereEqualTo("place", location)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection("favorites").document(user.uid)
                        .collection("events")
                        .document(document.id)
                        .delete()
                        .addOnSuccessListener {
                            showToast(context, "Favorite removed!")
                            Log.d("Firestore", "Favorite event successfully removed!")
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            showToast(context, "Failed to remove favorite.")
                            Log.w("Firestore", "Error removing favorite event", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                showToast(context, "Failed to check favorites.")
                Log.w("Firestore", "Error checking favorites", e)
            }
    } else {
        showToast(context, "No authenticated user found.")
    }
}

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
