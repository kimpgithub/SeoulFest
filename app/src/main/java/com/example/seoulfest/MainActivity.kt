package com.example.seoulfest

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.seoulfest.btmnavbtn.FavoritesScreen
import com.example.seoulfest.btmnavbtn.MapScreen
import com.example.seoulfest.btmnavbtn.MyPageScreen
import com.example.seoulfest.btmnavbtn.fetchFavorites
import com.example.seoulfest.btmnavbtn.mypage.EditProfileScreen
import com.example.seoulfest.detailscreen.DetailScreen
import com.example.seoulfest.login.LoginScreen
import com.example.seoulfest.main.BottomNavigationBar
import com.example.seoulfest.main.MainScreen
import com.example.seoulfest.main.MainViewModel
import com.example.seoulfest.models.CulturalEvent
import com.example.seoulfest.seoulfilter.SeoulFilter
import com.example.seoulfest.ui.theme.SeoulFestTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
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
                var upcomingEventCount by remember { mutableIntStateOf(0) }
                val viewModel: MainViewModel = viewModel()

                val updateUpcomingEventCount: () -> Unit = {
                    fetchFavorites({ events, count ->
                        upcomingEventCount = count
                        Log.d("MainActivity", "Updated Upcoming Event Count: $count") // Added log for verification
                    }, { exception ->
                        Log.w("MainActivity", "Failed to fetch favorites: ${exception.message}")
                    })
                }

                LaunchedEffect(Unit) {
                    updateUpcomingEventCount()
                    Log.d("MainActivity", "Calling loadNotificationSetting")
                    viewModel.loadNotificationSetting(this@MainActivity)
                }

                Scaffold(
                    bottomBar = { BottomBarVisibility(navController, updateUpcomingEventCount) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
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
                            MainScreen(navController, selectedDistricts, upcomingEventCount, viewModel)
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
                        composable("mypage") { MyPageScreen(navController, auth, viewModel) }
                        composable("edit_profile") { EditProfileScreen(navController, auth) }
                        composable("favorites") {
                            FavoritesScreen(navController) { count ->
                                upcomingEventCount = count
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomBarVisibility(navController: NavController, updateUpcomingEventCount: () -> Unit) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (currentRoute != "login") {
        BottomNavigationBar(navController = navController, updateUpcomingEventCount = updateUpcomingEventCount)
    }
}


private fun fetchFavorites(onResult: (List<CulturalEvent>, Int) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    currentUser?.let { user ->
        db.collection("favorites").document(user.uid)
            .collection("events")
            .get()
            .addOnSuccessListener { documents ->
                val events = documents.mapNotNull { document ->
                    document.toObject(CulturalEvent::class.java).apply {
                        id = document.id
                    }
                }
                val upcomingEventCount = calculateUpcomingEventCount(events)
                onResult(events, upcomingEventCount)
            }
            .addOnFailureListener { exception ->
                Log.w("MainActivity", "Error getting documents: ", exception)
                onResult(emptyList(), 0)
            }
    } ?: onResult(emptyList(), 0)
}

fun calculateUpcomingEventCount(events: List<CulturalEvent>): Int {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    val next30Days = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 30)
    }.time

    return events.count { event ->
        val eventDateRange = event.date?.split("~")
        if (eventDateRange != null && eventDateRange.size == 2) {
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(eventDateRange[0])
            startDate?.after(today) == true || startDate == today
        } else {
            false
        }
    }
}
