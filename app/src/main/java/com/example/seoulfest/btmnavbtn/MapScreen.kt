package com.example.seoulfest.btmnavbtn

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.seoulfest.R
import com.example.seoulfest.models.CulturalEvent
import com.example.seoulfest.network.SeoulCulturalEventService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavHostController) {
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val context = LocalContext.current

    when {
        locationPermissionState.status.isGranted -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("가까운 행사") },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                }
            ) {
                MapAndEventList()
            }
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
fun MapAndEventList() {
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val events = remember { mutableStateOf<List<CulturalEvent>>(emptyList()) }
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState()
    var selectedEventLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedEventLocationName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        Log.d("MapContent", "LaunchedEffect triggered")
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            val location = fusedLocationClient.lastLocation.await()
            location?.let {
                val newLocation = LatLng(it.latitude, it.longitude)
                userLocation = newLocation
                cameraPositionState.position = CameraPosition.fromLatLngZoom(newLocation, 15f)
                Log.d("MapContent", "User location updated: $newLocation")
                fetchEventsData(events, newLocation)
                isLoading = false
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            isLoading = false
        }
    }

    if (isLoading) {
        LoadingIndicator()
    } else {
        MapContent(
            userLocation = userLocation,
            selectedEventLocation = selectedEventLocation,
            selectedEventLocationName = selectedEventLocationName,
            cameraPositionState = cameraPositionState,
            events = events.value,
            onEventClick = { event ->
                event.lat?.toDoubleOrNull()?.let { lat ->
                    event.lng?.toDoubleOrNull()?.let { lng ->
                        val selectedLatLng = LatLng(lng, lat) // 위도와 경도 순서 수정
                        selectedEventLocation = selectedLatLng
                        selectedEventLocationName = event.place // Set the location name
                        Log.d("MapAndEventList", "Moving camera to: $selectedLatLng")
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                            selectedLatLng, 15f
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun LoadingIndicator() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Loading map...")
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun MapContent(
    userLocation: LatLng?,
    selectedEventLocation: LatLng?,
    selectedEventLocationName: String?,
    cameraPositionState: CameraPositionState,
    events: List<CulturalEvent>,
    onEventClick: (CulturalEvent) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Column {
        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp) // Adjust padding to account for the top bar
                .height(300.dp),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = userLocation != null),
            uiSettings = MapUiSettings(myLocationButtonEnabled = userLocation != null)
        ) {
            userLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "현재 위치",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                )
            }
            selectedEventLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = selectedEventLocationName, // Use the location name here
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }

            // Update the camera position to fit both markers
            if (userLocation != null && selectedEventLocation != null) {
                val bounds = LatLngBounds.Builder()
                    .include(userLocation)
                    .include(selectedEventLocation)
                    .build()

                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100)
                coroutineScope.launch {
                    cameraPositionState.move(cameraUpdate)
                }
            }
        }
        EventList(events, onEventClick)
    }
}

private suspend fun fetchEventsData(
    events: MutableState<List<CulturalEvent>>,
    userLocation: LatLng?
) {
    Log.d("MapContent", "Fetching events")
    val fetchedEvents = fetchEvents()

    userLocation?.let { location ->
        val sortedEvents = fetchedEvents.mapNotNull { event ->
            val eventLat = event.lat?.toDoubleOrNull()
            val eventLng = event.lng?.toDoubleOrNull()
            if (eventLat != null && eventLng != null) {
                val distance =
                    calculateDistance(location.latitude, location.longitude, eventLat, eventLng)
                event to distance
            } else {
                null
            }
        }.sortedBy { it.second }
            .map { it.first }
            .take(10)

        events.value = sortedEvents
        Log.d("MapContent", "Events updated: ${events.value.size}")
        events.value.forEach { event ->
            Log.d("MapContent", "Event location: ${event.lat}, ${event.lng}")
        }
    }
}

@Composable
fun EventList(events: List<CulturalEvent>, onEventClick: (CulturalEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(500.dp)
    ) {
        items(events) { event ->
            EventCard(event, onEventClick)
        }
    }
}

@Composable
fun EventCard(event: CulturalEvent, onEventClick: (CulturalEvent) -> Unit) {
    Card(
        onClick = {
            Log.d(
                "EventList",
                "Clicked event: ${event.title}, Lat: ${event.lat}, Lng: ${event.lng}"
            )
            onEventClick(event)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 이미지 로드
            val imageBitmap = loadPicture(
                url = event.mainImg ?: "",
                defaultImage = R.drawable.ic_launcher_foreground
            )
            imageBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp) // 여기서 size를 사용
                        .padding(end = 8.dp)
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = event.title ?: "No title",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.date ?: "No date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = event.place ?: "No place",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun loadPicture(url: String, @DrawableRes defaultImage: Int): ImageBitmap? {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(url) {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // Disable hardware bitmaps
            .build()

        val result = (loader.execute(request) as? SuccessResult)?.drawable
        bitmap = result?.toBitmap()?.asImageBitmap()
    }

    return bitmap
}

// Helper function to calculate distance between two coordinates
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val radius = 6371.0 // Earth radius in kilometers
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return radius * c
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
        val events = response.events ?: emptyList()
        events.sortedBy {
            it.date?.split("~")?.get(0)?.let { date ->
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
            }
        }.onEach { event ->
            Log.d(
                "fetchEvents",
                "Event: ${event.title}, Location: ${event.lat}, ${event.lng}"
            )
        }
    } catch (e: Exception) {
        Log.e("MapContent", "Failed to fetch events", e)
        emptyList()
    }
}
