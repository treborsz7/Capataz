// src/main/java/com/codegalaxy/barcodescanner/view/LoginScreen.kt
package com.codegalaxy.barcodescanner.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (String, String) -> Unit
) {
    android.util.Log.d("LoginScreen", "LoginScreen composable is being rendered")
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(WindowInsets.systemBars.asPaddingValues()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(Color.White, shape = RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            android.util.Log.d("LoginScreen", "Rendering login form")
            Text("Iniciar Sesión", fontSize = 28.sp, color = Color(0xFF1976D2))
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = usuario,
                onValueChange = {
                    usuario = it
                    android.util.Log.d("LoginScreen", "User input updated: $usuario")
                },
                label = { Text("Usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = contrasena,
                onValueChange = {
                    contrasena = it
                    android.util.Log.d("LoginScreen", "Password input updated")
                },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (errorMessage != null) {
                android.util.Log.d("LoginScreen", "Error message displayed: $errorMessage")
                Text(errorMessage, color = Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    android.util.Log.d("LoginScreen", "Login button clicked")
                    onLogin(usuario, contrasena)

                },
                //enabled = !isLoading && usuario.isNotBlank() && contrasena.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                if (isLoading) {
                    android.util.Log.d("LoginScreen", "Loading indicator displayed")
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Ingresar", color = Color.White)
                }
            }
        }
    }
}