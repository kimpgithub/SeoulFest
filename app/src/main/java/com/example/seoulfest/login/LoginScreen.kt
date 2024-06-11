package com.example.seoulfest.login

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.seoulfest.R
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(navController: NavHostController, auth: FirebaseAuth) {
    // 하드코딩된 자격 증명
    val email by remember { mutableStateOf("ex@ex.com") }
    val password by remember { mutableStateOf("111111") }
    val context = LocalContext.current

    val primaryColor = colorResource(id = R.color.colorPrimary)
    val onPrimaryColor = colorResource(id = R.color.colorTextPrimary)
    val secondaryColor = colorResource(id = R.color.colorAccent)
    val onSecondaryColor = colorResource(id = R.color.colorTextSecondary)
    val backgroundColor = colorResource(id = R.color.colorBackground)
    val onBackgroundColor = colorResource(id = R.color.colorTextPrimary)

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            secondary = secondaryColor,
            onSecondary = onSecondaryColor,
            background = backgroundColor,
            onBackground = onBackgroundColor,
        )
    ) {
        LoginContent(
            email = email,
            password = password,
            onLoginClick = {
                signInWithEmailAndPassword(auth, email, password, context) {
                    navController.navigate("main")
                }
            }
        )
    }
}
@Composable
fun LoginContent(email: String, password: String, onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmailTextField(email)
        Spacer(modifier = Modifier.height(8.dp))
        PasswordTextField(password)
        Spacer(modifier = Modifier.height(16.dp))
        LoginButton(onLoginClick)
    }
}
@Composable
fun EmailTextField(email: String) {
    TextField(
        value = email,
        onValueChange = { /* 하드코딩된 값이므로 필요 없음 */ },
        label = { Text("Email") },
        enabled = false // 편집 방지
    )
}
@Composable
fun PasswordTextField(password: String) {
    TextField(
        value = password,
        onValueChange = { /* 하드코딩된 값이므로 필요 없음 */ },
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        enabled = false // 편집 방지
    )
}
@Composable
fun LoginButton(onLoginClick: () -> Unit) {
    Button(
        onClick = onLoginClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text("Login")
    }
}

private fun signInWithEmailAndPassword(
    auth: FirebaseAuth,
    email: String,
    password: String,
    context: Context,
    onSuccess: () -> Unit
) {
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showToast(context, "Login successful")
                onSuccess()
            } else {
                showToast(context, "Login failed: ${task.exception?.message}")
                Log.e("LoginScreen", "Login failed", task.exception)
            }
        }
}
private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    LoginScreen(
        navController = NavHostController(LocalContext.current),
        auth = FirebaseAuth.getInstance()
    )
}
