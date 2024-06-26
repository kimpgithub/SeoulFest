package com.example.seoulfest.main_screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.seoulfest.R
import com.example.seoulfest.models.CulturalEvent
import java.net.URLEncoder
import kotlin.reflect.KFunction1

@Composable
fun MainScreen(
    navController: NavHostController,
    selectedDistricts: List<String>,
    selectedStartDate: String,
    selectedEndDate: String,
    viewModel: MainViewModel
) {
    val events by viewModel.events.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val upcomingEventCount by viewModel.upcomingEventCount.collectAsState()

    LaunchedEffect(selectedDistricts, selectedStartDate, selectedEndDate) {
        Log.d(
            "MainScreen",
            "Fetching events for date range: $selectedStartDate to $selectedEndDate and districts: $selectedDistricts"
        )
        viewModel.fetchEvents(
            apiKey = "74714163566b696d3431534b446673",
            selectedStartDate = selectedStartDate,
            selectedEndDate = selectedEndDate,
            selectedDistricts = selectedDistricts
        )
        viewModel.fetchFavoritesAndUpdateCount()
    }

    Scaffold(
        topBar = {
            TopBar(
                navController,
                selectedDistricts,
                selectedStartDate,
                selectedEndDate,
                notificationsEnabled,
                upcomingEventCount
            )
        }
    ) { innerPadding ->
        ContentColumn(
            innerPadding,
            selectedDistricts,
            selectedStartDate,
            selectedEndDate,
            navController,
            events
        )
    }
}

@Composable
fun LaunchLottieAnimation() {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("animation_launch.json"))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever // 무한 반복 설정
    )

    if (composition == null) {
        Log.e("Lottie", "Failed to load composition")
    }

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.size(60.dp)
    )
}

@Composable
fun NotificationIcon(navController: NavHostController, upcomingEventCount: Int) {
    Box {
        IconButton(onClick = { navController.navigate("favorites") }) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Favorites",
                tint = colorResource(id = R.color.colorTextPrimary)
            )
        }
        if (upcomingEventCount > 0) {
            Badge(
                modifier = Modifier.align(Alignment.TopEnd),
                containerColor = colorResource(id = R.color.colorAccent)
            ) {
                Text(
                    text = upcomingEventCount.toString(),
                    color = colorResource(id = R.color.colorTextPrimary),
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}

@Composable
fun SelectedDistrictsRow(selectedDistricts: List<String>, navController: NavHostController) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 8.dp)
    ) {
        selectedDistricts.forEach { district ->
            DistrictItem(district, selectedDistricts, navController)
        }
    }
}

@Composable
fun DistrictItem(
    district: String,
    selectedDistricts: List<String>,
    navController: NavHostController
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .background(colorResource(id = R.color.colorAccent))
            .padding(8.dp)
            .padding(end = 8.dp)
    ) {
        Text(
            text = district,
            color = colorResource(id = R.color.colorTextPrimary),
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            "X",
            color = colorResource(id = R.color.colorTextPrimary),
            modifier = Modifier.clickable {
                val newSelectedDistricts =
                    selectedDistricts.toMutableList().apply { remove(district) }
                val newSelectedDistrictsStr = newSelectedDistricts.joinToString(",")
                navController.navigate("main?selectedDistricts=$newSelectedDistrictsStr") {
                    popUpTo("main") { inclusive = true }
                }
            }
        )
    }
}

@Composable
fun EventList(events: List<CulturalEvent>, navController: NavHostController) {
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


@Composable
fun BottomNavigationBar(
    navController: NavController,
    updateUpcomingEventCount: KFunction1<(Int) -> Unit, Unit>
) {
    NavigationBar(
        containerColor = colorResource(id = R.color.colorPrimary), // NavigationBar 배경 색상
        contentColor = colorResource(id = R.color.colorTextPrimary) // NavigationBar 텍스트 색상
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentDestination?.route == "main",
            onClick = {
                navController.navigate("main") {
                    launchSingleTop = true
                }
                updateUpcomingEventCount {
                    // Count updated
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorResource(id = R.color.colorAccent), // 선택된 아이템의 아이콘 색상
                selectedTextColor = colorResource(id = R.color.colorAccent), // 선택된 아이템의 텍스트 색상
                unselectedIconColor = colorResource(id = R.color.colorTextSecondary), // 선택되지 않은 아이템의 아이콘 색상
                unselectedTextColor = colorResource(id = R.color.colorTextSecondary) // 선택되지 않은 아이템의 텍스트 색상
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
            label = { Text("Map") },
            selected = currentDestination?.route == "map",
            onClick = { navController.navigate("map") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorResource(id = R.color.colorAccent), // 선택된 아이템의 아이콘 색상
                selectedTextColor = colorResource(id = R.color.colorTextPrimary), // 선택된 아이템의 텍스트 색상
                unselectedIconColor = colorResource(id = R.color.colorTextSecondary), // 선택되지 않은 아이템의 아이콘 색상
                unselectedTextColor = colorResource(id = R.color.colorTextSecondary) // 선택되지 않은 아이템의 텍스트 색상
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
            label = { Text("Favorites") },
            selected = currentDestination?.route == "favorites",
            onClick = { navController.navigate("favorites") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorResource(id = R.color.colorAccent), // 선택된 아이템의 아이콘 색상
                selectedTextColor = colorResource(id = R.color.colorTextPrimary), // 선택된 아이템의 텍스트 색상
                unselectedIconColor = colorResource(id = R.color.colorTextSecondary), // 선택되지 않은 아이템의 아이콘 색상
                unselectedTextColor = colorResource(id = R.color.colorTextSecondary) // 선택되지 않은 아이템의 텍스트 색상
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "My Page") },
            label = { Text("My Page") },
            selected = currentDestination?.route == "mypage",
            onClick = { navController.navigate("mypage") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorResource(id = R.color.colorAccent), // 선택된 아이템의 아이콘 색상
                selectedTextColor = colorResource(id = R.color.colorTextPrimary), // 선택된 아이템의 텍스트 색상
                unselectedIconColor = colorResource(id = R.color.colorTextSecondary), // 선택되지 않은 아이템의 아이콘 색상
                unselectedTextColor = colorResource(id = R.color.colorTextSecondary) // 선택되지 않은 아이템의 텍스트 색상
            )
        )
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
                .size(120.dp)
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




