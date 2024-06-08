package com.example.seoulfest

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.seoulfest.btmnavbtn.MapScreen
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
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
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
}

