package com.example.seoulfest.btmnavbtn

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.seoulfest.R
import com.example.seoulfest.main.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(navController: NavHostController, auth: FirebaseAuth, viewModel: MainViewModel) {
    val user = auth.currentUser
    val context = LocalContext.current


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("마이 페이지") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                item { ProfileSection(user) { navController.navigate("edit_profile") } }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { NotificationSettingsSection(viewModel) }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { AppSettingsSection(navController) }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { LogoutSection(navController, context, auth) }
            }
        }
    )
}
@Composable
fun ProfileSection(user: FirebaseUser?, onEditProfile: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (user?.photoUrl != null) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
        } else {
            // Use painterResource to load the default image
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
        }
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
        Button(onClick = onEditProfile) {
            Text("Edit Profile")
        }
    }
}

@Composable
fun NotificationSettingsSection(viewModel: MainViewModel) {
    val context = LocalContext.current
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    Column {
        Text("Notification Settings", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Switch(
            checked = notificationsEnabled,
            onCheckedChange = { isChecked ->
                viewModel.saveNotificationSetting(context, isChecked)
                Log.d("MyPageScreen", "notificationsEnabled changed to: $isChecked")
            },
            modifier = Modifier.padding(8.dp)
        )
        Text("Receive event notifications", style = MaterialTheme.typography.bodyMedium)
    }
}

    // 알림 설정 상태를 로드하는 함수 (SharedPreferences 사용 예시)
private fun loadNotificationSetting(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("notifications_enabled", true) // 기본값을 true로 설정
}

//TODO: DataStore

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
