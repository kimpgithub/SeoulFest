package com.example.seoulfest

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.seoulfest.btmnavbtn.MyPageScreen
import com.example.seoulfest.detailscreen.DetailScreen
import com.example.seoulfest.login.LoginScreen
import com.example.seoulfest.main.MainScreen
import com.example.seoulfest.models.CulturalEvent
import com.example.seoulfest.network.SeoulCulturalEventService
import com.example.seoulfest.seoulfilter.SeoulFilter
import com.example.seoulfest.ui.theme.SeoulFestTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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
                        SeoulFilter(navController, selectedDistricts)
                    }
                    composable("map") { MapScreen(navController) }
                    composable("mypage") { MyPageScreen(navController, auth) }
                }
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun MapScreen(navController: NavHostController) {
        val locationPermissionState =
            rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
        val context = LocalContext.current

        when {
            locationPermissionState.status.isGranted -> {
                MapContent()
            }

            locationPermissionState.status.shouldShowRationale -> {
                PermissionRationale(permissionState = locationPermissionState)
            }

            else -> {
                PermissionRationale(permissionState = locationPermissionState)
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun PermissionRationale(permissionState: PermissionState) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Location permission is required to show the map.")
            Button(onClick = { permissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    }

    @Composable
    fun MapContent() {
        var userLocation by remember {
            mutableStateOf(
                LatLng(
                    37.5665,
                    126.9780
                )
            )
        } // Default to Seoul
        var events by remember { mutableStateOf<List<CulturalEvent>>(emptyList()) }
        val context = LocalContext.current
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(userLocation, 15f)
        }

        LaunchedEffect(Unit) {
            Log.d("MapContent", "LaunchedEffect triggered")
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(userLocation, 15f)
                        Log.d("MapContent", "User location updated: $userLocation")

                        // Launch a coroutine to fetch events
                        launch {
                            Log.d("MapContent", "Fetching events")
                            val fetchedEvents = fetchEvents()
                            events = fetchedEvents.sortedBy {
                                it.getDistanceFrom(userLocation)
                            }.take(10)
                            Log.d("MapContent", "Fetched events: ${events.size}")

                            // Log each event's location
                            events.forEach { event ->
                                Log.d("MapContent", "Event location: ${event.lat}, ${event.lng}")
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        Log.d("MapContent", "Rendering GoogleMap")
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false)
        ) {
            // Add user location marker
            Marker(
                state = MarkerState(position = userLocation),
                title = "You are here",
                icon = BitmapDescriptorFactory.fromResource(R.drawable.marker)
            )

            // Add event markers
            events.forEach { event ->
                event.lat?.toDoubleOrNull()?.let { lat ->
                    event.lng?.toDoubleOrNull()?.let { lng ->
                        Marker(
                            state = MarkerState(position = LatLng(lat, lng)),
                            title = event.title,
                            snippet = event.date,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                            onClick = { marker ->
                                marker.showInfoWindow()
                                true
                            }
                        )
                    }
                }
            }
        }
    }
    // Function to fetch events
    private suspend fun fetchEvents(): List<CulturalEvent> {
        val apiService = SeoulCulturalEventService.create()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return try {
            val response = apiService.getEvents(
                apiKey = "74714163566b696d3431534b446673",
                type = "xml",
                service = "culturalEventInfo",
                startIndex = 1,
                endIndex = 100,
                date = today
            )
            val events = response.events?.sortedBy {
                it.date?.split("~")?.get(0)?.let { date ->
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                }
            } ?: emptyList()
            events.forEach { event ->
                Log.d("fetchEvents", "Event: ${event.title}, Location: ${event.lat}, ${event.lng}")
            }

            events
        } catch (e: Exception) {
            Log.e("MapContent", "Failed to fetch events", e)
            emptyList()
        }
    }

    // Extension function to calculate distance
    private fun CulturalEvent.getDistanceFrom(location: LatLng): Double {
        val eventLat = this.lat?.toDoubleOrNull() ?: return Double.MAX_VALUE
        val eventLng = this.lng?.toDoubleOrNull() ?: return Double.MAX_VALUE
        val userLat = location.latitude
        val userLng = location.longitude

        val earthRadius = 6371e3 // Radius of the earth in meters

        val latDiff = Math.toRadians(eventLat - userLat)
        val lngDiff = Math.toRadians(eventLng - userLng)

        val a =
            sin(latDiff / 2).pow(2.0) + cos(Math.toRadians(userLat)) * cos(Math.toRadians(eventLat)) * sin(
                lngDiff / 2
            ).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }
}