package com.example.seoulfest

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.seoulfest.ui.theme.SeoulFestTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.seoulfest.models.CulturalEvent
import com.example.seoulfest.network.SeoulCulturalEventService
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        setContent {
            SeoulFestTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "login") {
                    composable("login") { LoginScreen(navController, auth) }
                    composable("main") { MainScreen(navController) }
                    composable(
                        "detail?title={title}&date={date}&location={location}&pay={pay}&imageUrl={imageUrl}",
                        arguments = listOf(
                            navArgument("title") { type = NavType.StringType },
                            navArgument("date") { type = NavType.StringType },
                            navArgument("location") { type = NavType.StringType },
                            navArgument("pay") { type = NavType.StringType },
                            navArgument("imageUrl") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        DetailScreen(
                            title = backStackEntry.arguments?.getString("title") ?: "",
                            date = backStackEntry.arguments?.getString("date") ?: "",
                            location = backStackEntry.arguments?.getString("location") ?: "",
                            pay = backStackEntry.arguments?.getString("pay") ?: "",
                            imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: "",
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(navController: NavHostController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            signInWithEmailAndPassword(auth, email, password, context) {
                navController.navigate("main")
            }
        }) {
            Text("Login")
        }
    }
}

fun signInWithEmailAndPassword(
    auth: FirebaseAuth,
    email: String,
    password: String,
    context: android.content.Context,
    onSuccess: () -> Unit
) {
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                onSuccess()
            } else {
                Toast.makeText(
                    context,
                    "Login failed: ${task.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("LoginScreen", "Login failed", task.exception)
            }
        }
}

@Composable
fun MainScreen(navController: NavHostController) {
    var events by remember { mutableStateOf<List<CulturalEvent>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val apiService = SeoulCulturalEventService.create()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        Log.d("MainScreen", "Today's Date: $today") // 로그 추가
        try {
            val response = apiService.getEvents(
                apiKey = "",
                type = "xml",
                service = "culturalEventInfo",
                startIndex = 1,
                endIndex = 100,
                date = today // 오늘 날짜 이후 데이터 요청
            )
            val sortedEvents = response.events?.filter {
                val startDate = it.date?.split("~")?.get(0)
                startDate != null && SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(
                    startDate
                )!! >= SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(today)
            }?.sortedBy {
                it.date?.split("~")?.get(0)
                    ?.let { it1 -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it1) }
            }

            events = sortedEvents?.take(10) ?: emptyList() // 오름차순 정렬 후 10개 데이터 가져오기
            Log.d("MainScreen", "Fetched Events: $events")
        } catch (e: Exception) {
            Log.e("MainScreen", "Failed to fetch events", e)
            Toast.makeText(context, "Failed to fetch events", Toast.LENGTH_SHORT).show()
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        EventList(events, navController)
    }
}

@Composable
fun EventItem(
    title: String,
    date: String,
    time: String,
    location: String,
    pay: String,
    imageUrl: String,
    navController: NavHostController
) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
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
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .padding(end = 8.dp)
        )

        Column {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(color = Color.Black))
            Text(date, style = MaterialTheme.typography.bodySmall.copy(color = Color.Black))
            Text(time, style = MaterialTheme.typography.bodySmall.copy(color = Color.Black))
            Text(location, style = MaterialTheme.typography.bodySmall.copy(color = Color.Black))
            Text(pay, style = MaterialTheme.typography.bodySmall.copy(color = Color.Black))
        }
    }
}

@Composable
fun EventList(events: List<CulturalEvent>, navController: NavHostController) {
    Log.d("EventList", "Events to display: $events")

    Column {
        events.forEach { event ->
            EventItem(
                title = event.title ?: "",
                date = event.date ?: "",
                time = "",
                location = event.place ?: "",
                pay = event.useFee ?: "",
                imageUrl = event.mainImg ?: "",
                navController = navController
            )
        }
    }
}

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
    val decodedTitle = URLDecoder.decode(title, "UTF-8")
    val decodedDate = URLDecoder.decode(date, "UTF-8")
    val decodedLocation = URLDecoder.decode(location, "UTF-8")
    val decodedPay = URLDecoder.decode(pay, "UTF-8")
    val decodedImageUrl = URLDecoder.decode(imageUrl, "UTF-8")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("Detail") },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { /* TODO: 즐겨찾기 기능 구현 */ }) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite")
                }
            }
        )
        AsyncImage(
            model = decodedImageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SeoulFestTheme {
        MainScreen(rememberNavController())
    }
}
