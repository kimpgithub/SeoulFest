package com.example.seoulfest

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.seoulfest.models.CulturalEvent
import com.example.seoulfest.network.SeoulCulturalEventService
import coil.compose.AsyncImage
import java.util.Date
import java.util.Locale

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
                    composable("main") { MainScreen() }
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
                Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                Log.e("LoginScreen", "Login failed", task.exception)
            }
        }
}

@Composable
fun MainScreen() {
    var events by remember { mutableStateOf<List<CulturalEvent>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val apiService = SeoulCulturalEventService.create()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        Log.d("MainScreen", "Today's Date: $today") // 로그 추가
        try {
            val response = apiService.getEvents(
                apiKey = "API키를 입력하는 자리",
                type = "xml",
                service = "culturalEventInfo",
                startIndex = 1,
                endIndex = 100,
                date = today // 오늘 날짜 이후 데이터 요청
            )
            val sortedEvents = response.events?.filter {
                val startDate = it.date?.split("~")?.get(0)
                startDate != null && SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startDate) >= SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(today)
            }?.sortedBy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date?.split("~")?.get(0)) }

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
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        EventList(events)
    }
}

@Composable
fun EventItem(title: String, date: String, time: String, location: String, pay: String, imageUrl: String) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .background(Color.White)  // 박스를 흰색으로 설정
            .padding(16.dp)
            .clickable { /* 이벤트 상세 페이지로 이동 */ }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .padding(end = 8.dp)
        )

        Column {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(color = Color.Black))  // 글씨는 검은색
            Text(date, style = MaterialTheme.typography.bodySmall.copy(color = Color.Black))  // 글씨는 검은색
            Text(time, style = MaterialTheme.typography.bodySmall.copy(color = Color.Black))  // 글씨는 검은색
            Text(location, style = MaterialTheme.typography.bodySmall.copy(color = Color.Black))  // 글씨는 검은색
            Text(pay, style = MaterialTheme.typography.bodySmall.copy(color = Color.Black))  // 글씨는 검은색
        }
    }
}

@Composable
fun EventList(events: List<CulturalEvent>) {
    Log.d("EventList", "Events to display: $events")

    Column {
        events.forEach { event ->
            EventItem(
                title = event.title ?: "",
                date = event.date ?: "",
                time = "",  // 시간 정보가 없어서 빈 문자열로 설정
                location = event.place ?: "",
                pay = event.useFee ?: "",
                imageUrl = event.mainImg ?: ""  // 이미지 URL 추가
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SeoulFestTheme {
        MainScreen()
    }
}
