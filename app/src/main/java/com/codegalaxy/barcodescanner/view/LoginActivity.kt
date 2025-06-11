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
                        onLogin = { nombreUsuario, pass ->
                            android.util.Log.d("LoginActivity", "Login button clicked with user: $nombreUsuario, pass: $pass")
                            if (nombreUsuario.isBlank()) {
                                android.util.Log.e("LoginActivity", "Usuario vacío, no se llama a la API")
                                return@LoginScreen
                            }
                            android.util.Log.d("LoginActivity", "Calling loginPlano API...")
                            ApiClient.apiService.loginPlano(nombreUsuario = nombreUsuario, pass = pass).enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
                                override fun onResponse(
                                    call: retrofit2.Call<okhttp3.ResponseBody>,
                                    response: retrofit2.Response<okhttp3.ResponseBody>
                                ) {
                                    val rawBody = response.body()?.string()
                                    val errorBody = response.errorBody()?.string()
                                    android.util.Log.d("LoginActivity", "loginPlano onResponse: isSuccessful=${response.isSuccessful}, code=${response.code()}, rawBody=$rawBody, errorBody=$errorBody")
                                    if (response.isSuccessful && rawBody != null) {
                                        // Guardar el rawBody como token en el store
                                        val prefs = getSharedPreferences("QRCodeScannerPrefs", MODE_PRIVATE)
                                        prefs.edit().putString("token", rawBody).apply()
                                        android.util.Log.d("LoginActivity", "Token guardado en SharedPreferences: $rawBody")
                                        // Login exitoso, navega a MainActivity
                                        runOnUiThread {
                                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        }
                                    } else {
                                        android.util.Log.e("LoginActivity", "Login failed: $errorBody or token not found in response")
                                        runOnUiThread {
                                            // Muestra mensaje de error
                                        }
                                    }
                                }

                                override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                                    android.util.Log.e("LoginActivity", "loginPlano onFailure: ${t.message}", t)
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