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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    composable(
                        "main?selectedDistricts={selectedDistricts}",
                        arguments = listOf(navArgument("selectedDistricts") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        })
                    ) { backStackEntry ->
                        val selectedDistricts = backStackEntry.arguments?.getString("selectedDistricts")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
                        MainScreen(navController, selectedDistricts)
                    }
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
                    composable(
                        "seoul?selectedDistricts={selectedDistricts}",
                        arguments = listOf(
                            navArgument("selectedDistricts") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = ""
                            }
                        )
                    ) { backStackEntry ->
                        val selectedDistricts = backStackEntry.arguments?.getString("selectedDistricts")?.split(",") ?: emptyList()
                        SeoulScreen(navController, selectedDistricts)
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

    private fun signInWithEmailAndPassword(
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
    fun MainScreen(navController: NavHostController, selectedDistricts: List<String>) {
        var events by remember { mutableStateOf<List<CulturalEvent>>(emptyList()) }
        val context = LocalContext.current

        LaunchedEffect(selectedDistricts) {
            val apiService = SeoulCulturalEventService.create()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            Log.d("MainScreen", "Today's Date: $today") // 로그 추가
            Log.d("MainScreen", "Selected Districts: $selectedDistricts") // 선택된 구 로그 추가
            try {
                val response = apiService.getEvents(
                    apiKey = "",
                    type = "xml",
                    service = "culturalEventInfo",
                    startIndex = 1,
                    endIndex = 100,
                    date = today // 오늘 날짜 이후 데이터 요청
                )
                val filteredEvents = response.events?.filter { event ->
                    selectedDistricts.isEmpty() || selectedDistricts.any { district ->
                        event.guname?.contains(district) == true
                    }
                }
                val sortedEvents = filteredEvents?.sortedBy {
                    it.date?.split("~")?.get(0)
                        ?.let { date ->
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                        }
                }

                events = sortedEvents?.take(10) ?: emptyList() // 오름차순 정렬 후 10개 데이터 가져오기
            } catch (e: Exception) {
                Log.e("MainScreen", "Failed to fetch events", e)
                Toast.makeText(context, "Failed to fetch events", Toast.LENGTH_SHORT).show()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Button(onClick = {
                    val selectedDistrictsStr = selectedDistricts.joinToString(",")
                    navController.navigate("seoul?selectedDistricts=$selectedDistrictsStr")
                }) {
                    Text("서울")
                }
            }

            LazyRow(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                items(selectedDistricts) { district ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .background(Color.Gray)
                            .padding(8.dp)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = district,
                            color = Color.White,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "X",
                            color = Color.White,
                            modifier = Modifier.clickable {
                                // 선택된 구를 삭제하고 새로고침
                                val newSelectedDistricts = selectedDistricts.toMutableList().apply { remove(district) }
                                val newSelectedDistrictsStr = newSelectedDistricts.joinToString(",")
                                navController.navigate("main?selectedDistricts=$newSelectedDistrictsStr") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }

            LazyColumn {
                items(events) { event ->
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
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SeoulScreen(navController: NavHostController, initialSelectedDistricts: List<String>) {
        var selectedDistricts by remember { mutableStateOf(initialSelectedDistricts.toMutableList()) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top bar with "서울특별시" text and X button
            TopAppBar(
                title = { Text("서울특별시") },
                actions = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )

            // Grid of district names
            val districts = listOf(
                "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구",
                "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구",
                "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구",
                "중구", "중랑구"
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(districts) { district ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(8.dp)
                            .width(80.dp) // 고정된 너비 설정
                            .height(40.dp) // 고정된 높이 설정
                            .clickable {
                                selectedDistricts = if (selectedDistricts.contains(district)) {
                                    selectedDistricts.toMutableList().apply { remove(district) }
                                } else {
                                    selectedDistricts.toMutableList().apply { add(district) }
                                }
                            }
                            .background(if (selectedDistricts.contains(district)) Color.Gray else Color.Transparent)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = district,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp // 텍스트 크기 조정
                        )
                    }
                }
            }

            // Confirm button at the bottom
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val selectedDistrictsStr = selectedDistricts.joinToString(",")
                    navController.navigate("main?selectedDistricts=$selectedDistrictsStr") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("확인")
            }
        }
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

