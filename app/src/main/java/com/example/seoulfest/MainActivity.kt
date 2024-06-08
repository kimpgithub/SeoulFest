package com.example.seoulfest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.seoulfest.btmnavbtn.MapScreen
import com.example.seoulfest.btmnavbtn.MyPageScreen
import com.example.seoulfest.detailscreen.DetailScreen
import com.example.seoulfest.login.LoginScreen
import com.example.seoulfest.main.BottomNavigationBar
import com.example.seoulfest.main.MainScreen
import com.example.seoulfest.seoulfilter.SeoulFilter
import com.example.seoulfest.ui.theme.SeoulFestTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        setContent {
            SeoulFestTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { BottomNavigationBar(navController) }
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
}
