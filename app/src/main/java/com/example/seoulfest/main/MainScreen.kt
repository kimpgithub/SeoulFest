package com.example.seoulfest.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(navController: NavHostController, selectedDistricts: List<String>) {
    val viewModel: MainViewModel = viewModel()
    val events by viewModel.events.collectAsState()

    LaunchedEffect(selectedDistricts) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewModel.fetchEvents(apiKey = "74714163566b696d3431534b446673", today = today, selectedDistricts = selectedDistricts)
    }

    Scaffold{ innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
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
                    Text("서울 지역 선택")
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
fun BottomNavigationBar(navController: NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentDestination?.route == "main",
            onClick = { navController.navigate("main") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
            label = { Text("Map") },
            selected = currentDestination?.route == "map",
            onClick = { navController.navigate("map") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
            label = { Text("Favorites") },
            selected = currentDestination?.route == "favorites",
            onClick = { navController.navigate("favorites") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "My Page") },
            label = { Text("My Page") },
            selected = currentDestination?.route == "mypage",
            onClick = { navController.navigate("mypage") }
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
