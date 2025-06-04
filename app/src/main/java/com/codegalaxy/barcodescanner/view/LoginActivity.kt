// src/main/java/com/codegalaxy/barcodescanner/view/LoginActivity.kt
package com.codegalaxy.barcodescanner.view

import ApiService
import LoginPlanoResponse
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.codegalaxy.barcodescanner.ui.theme.BarCodeScannerTheme

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        android.util.Log.d("LoginActivity", "LoginActivity is being created")
        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    //val apiService = retrofit.create(ApiService::class.java)
                    android.util.Log.d("LoginActivity", "LoginScreen is being rendered")
                    LoginScreen(

                        onLoginSuccess = {
                            android.util.Log.d("LoginActivity", "Login successful, navigating to MainActivity")
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        },
                        isLoading = false,
                        errorMessage = null,
                        onLogin = { usuario, contrasena ->

                            android.util.Log.d("LoginActivity", "Login button clicked with user: $usuario")
                            android.util.Log.d("LoginActivity", "Calling loginPlano API...")
                            ApiClient.apiService.loginPlano(usuario, contrasena).enqueue(object : retrofit2.Callback<LoginPlanoResponse> {
                                override fun onResponse(
                                    call: retrofit2.Call<LoginPlanoResponse>,
                                    response: retrofit2.Response<LoginPlanoResponse>
                                ) {
                                    android.util.Log.d("LoginActivity", "loginPlano onResponse: isSuccessful=${response.isSuccessful}, code=${response.code()}, body=${response.body()}")
                                    if (response.isSuccessful && response.body()?.token != null) {
                                        android.util.Log.d("LoginActivity", "Login successful, token: ${response.body()?.token}")
                                        // Login exitoso, navega a MainActivity
                                        runOnUiThread {
                                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        }
                                    } else {
                                        android.util.Log.e("LoginActivity", "Login failed: ${response.errorBody()?.string()} or token null")
                                        // Maneja error de login
                                        runOnUiThread {
                                            // Muestra mensaje de error
                                        }
                                    }
                                }

                                override fun onFailure(call: retrofit2.Call<LoginPlanoResponse>, t: Throwable) {
                                    android.util.Log.e("LoginActivity", "loginPlano onFailure: ${t.message}", t)
                                    // Maneja error de red
                                    runOnUiThread {
                                        // Muestra mensaje de error
                                    }
                                }
                            })
                        }
                    )
                }
            }
        }
    }
}