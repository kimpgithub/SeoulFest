package com.example.seoulfest.btmnavbtn

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(navController: NavHostController, auth: FirebaseAuth) {
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
