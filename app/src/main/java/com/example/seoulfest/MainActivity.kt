package com.example.seoulfest

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.firebase.auth.FirebaseUser
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng


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
                        val selectedDistricts =
                            backStackEntry.arguments?.getString("selectedDistricts")?.split(",")
                                ?.filter { it.isNotEmpty() } ?: emptyList()
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
                        val selectedDistricts =
                            backStackEntry.arguments?.getString("selectedDistricts")?.split(",")
                                ?: emptyList()
                        SeoulScreen(navController, selectedDistricts)
                    }
                    composable("map") { MapScreen(navController) }
                    composable("mypage") { MyPageScreen(navController, auth) }
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
        context: Context,
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


        Scaffold(
            bottomBar = {
                BottomNavigationBar(navController)
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // 기존 UI 코드
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Button(onClick = {
                        val selectedDistrictsStr = selectedDistricts.joinToString(",")
                        navController.navigate("seoul?selectedDistricts=$selectedDistrictsStr")
                    }) {
                        Text("서울")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                ) {
                    selectedDistricts.forEach { district ->
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
                                    val newSelectedDistricts =
                                        selectedDistricts.toMutableList().apply { remove(district) }
                                    val newSelectedDistrictsStr =
                                        newSelectedDistricts.joinToString(",")
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
    }

    @Composable
    fun BottomNavigationBar(navController: NavHostController) {
        NavigationBar {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            NavigationBarItem(
                icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                label = { Text("Map") },
                selected = currentDestination?.route == "map",
                onClick = { navController.navigate("map") }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Favorite, contentDescription = "Interested Events") },
                label = { Text("Bookmarks") },
                selected = currentDestination?.route == "interested_events",
                onClick = { navController.navigate("interested_events") }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "My Page") },
                label = { Text("My Page") },
                selected = currentDestination?.route == "mypage",
                onClick = { navController.navigate("mypage") }
            )
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
                                    selectedDistricts
                                        .toMutableList()
                                        .apply { remove(district) }
                                } else {
                                    selectedDistricts
                                        .toMutableList()
                                        .apply { add(district) }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun MapScreen(navController: NavHostController) {
        val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

        if (locationPermissionState.status.isGranted) {
            val seoul = LatLng(37.5665, 126.9780) // 서울의 위도와 경도
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(seoul, 10f)
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            )
        } else {
            PermissionNotGrantedContent(locationPermissionState)
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun PermissionNotGrantedContent(permissionState: PermissionState) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val textToShow = if (permissionState.status.shouldShowRationale) {
                "Location access is required to show the map. Please grant the permission."
            } else {
                "Location permission required for this feature to be available. Please grant the permission."
            }
            Text(textToShow)
            Button(onClick = { permissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MyPageScreen(navController: NavHostController, auth: FirebaseAuth) {
        val user = auth.currentUser
        val context = LocalContext.current


        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Page") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            content = { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    ProfileSection(user)
                    Spacer(modifier = Modifier.height(16.dp))
                    NotificationSettingsSection()
                    Spacer(modifier = Modifier.height(16.dp))
                    AppSettingsSection(navController)
                    Spacer(modifier = Modifier.height(16.dp))
                    LogoutSection(navController, context, auth) // NavController 인자로 전달
                }
            }
        )
    }

    @Composable
    fun ProfileSection(user: FirebaseUser?) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = user?.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .clickable { /* 프로필 사진 변경 기능 */ }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = user?.displayName ?: "User Name",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user?.email ?: "user@example.com",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { /* 프로필 수정 기능 */ }) {
                Text("Edit Profile")
            }
        }
    }

    @Composable
    fun NotificationSettingsSection() {
        val context = LocalContext.current
        var notificationsEnabled by remember { mutableStateOf(false) }

        // 초기화 및 상태 로드
        LaunchedEffect(Unit) {
            // 여기에 SharedPreferences 또는 데이터 저장소에서 알림 설정 상태를 로드하는 코드를 추가합니다.
            notificationsEnabled = loadNotificationSetting(context)
        }

        Column {
            Text("Notification Settings", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { isChecked ->
                    notificationsEnabled = isChecked
                    saveNotificationSetting(context, isChecked)
                },
                modifier = Modifier.padding(8.dp)
            )
            Text("Receive event notifications", style = MaterialTheme.typography.bodyMedium)
        }
    }

    // 알림 설정 상태를 로드하는 함수 (SharedPreferences 사용 예시)
    private fun loadNotificationSetting(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("notifications_enabled", false)
    }

    // 알림 설정 상태를 저장하는 함수 (SharedPreferences 사용 예시)
    private fun saveNotificationSetting(context: Context, isEnabled: Boolean) {
        val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("notifications_enabled", isEnabled).apply()
    }

    @Composable
    fun AppSettingsSection(navController: NavHostController) {
        Column {
            Text("App Settings", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { /* 테마 설정 화면으로 이동 */ }) {
                Text("Theme Settings")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { /* 언어 설정 화면으로 이동 */ }) {
                Text("Language Settings")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { /* 알림 음량 설정 화면으로 이동 */ }) {
                Text("Notification Volume Settings")
            }
        }
    }

    @Composable
    fun LogoutSection(navController: NavHostController, context: Context, auth: FirebaseAuth) {
        Column {
            Text("Account", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                auth.signOut()
                Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                navController.navigate("login") {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }) {
                Text("Logout")
            }
        }
    }

}