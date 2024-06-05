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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(navController: NavHostController, auth: FirebaseAuth) {
    // Hardcoded credentials for development
    val email by remember { mutableStateOf("ex@ex.com") }
    val password by remember { mutableStateOf("111111") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = email,
            onValueChange = { /* No action needed for hardcoded values */ },
            label = { Text("Email") },
            enabled = false // Disable the TextField to prevent editing
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { /* No action needed for hardcoded values */ },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            enabled = false // Disable the TextField to prevent editing
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            signInWithEmailAndPassword(auth, email, password, context) {
                navController.navigate("main")
            }
        }) {
            Text("Login")
        }
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
                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                onSuccess()
            } else {
                Toast.makeText(
                    context,
                    "Login failed: ${task.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("LoginScreen", "Login failed", task.exception)
            }
        }
}
