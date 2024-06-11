package com.example.seoulfest

import android.os.Bundle
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.seoulfest.btm_nav_btn.favorites_screen.FavoritesScreen
import com.example.seoulfest.btm_nav_btn.map_screen.MapScreen
import com.example.seoulfest.btm_nav_btn.mypage_screen.MyPageScreen
import com.example.seoulfest.btm_nav_btn.mypage_screen.EditProfileScreen
import com.example.seoulfest.detail_screen.DetailScreen
import com.example.seoulfest.login.LoginScreen
import com.example.seoulfest.main_screen.BottomNavigationBar
import com.example.seoulfest.main_screen.MainScreen
import com.example.seoulfest.main_screen.MainViewModel
import com.example.seoulfest.seoulfilter_screen.SeoulFilter
import com.example.seoulfest.ui.theme.SeoulFestTheme
import com.example.seoulfest.utils.FirebaseUtils
import com.google.firebase.auth.FirebaseAuth
import kotlin.reflect.KFunction1

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseUtils.initializeFirebase(this)
        auth = FirebaseAuth.getInstance()

        setContent {
            SeoulFestTheme {
                MainScreenContent(auth)
            }
        }
    }
}

@Composable
fun MainScreenContent(auth: FirebaseAuth) {
    val navController = rememberNavController()
    var upcomingEventCount by remember { mutableIntStateOf(0) }
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        updateUpcomingEventCount { count ->
            upcomingEventCount = count
        }
        viewModel.loadNotificationSetting(context)
    }

    Scaffold(
        bottomBar = { BottomBarVisibility(navController, ::updateUpcomingEventCount) }
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
                val selectedDistricts = backStackEntry.arguments?.getString("selectedDistricts")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
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
                val selectedDistricts = backStackEntry.arguments?.getString("selectedDistricts")?.split(",") ?: emptyList()
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

@Composable
fun BottomBarVisibility(navController: NavController, updateUpcomingEventCount: KFunction1<(Int) -> Unit, Unit>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (currentRoute != "login") {
        BottomNavigationBar(navController = navController, updateUpcomingEventCount = updateUpcomingEventCount)
    }
}

fun updateUpcomingEventCount(onResult: (Int) -> Unit) {
    FirebaseUtils.fetchFavorites { _, count ->
        onResult(count)
    }
}
