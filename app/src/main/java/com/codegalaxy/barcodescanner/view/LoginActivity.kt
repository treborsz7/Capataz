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
        val prefs = getSharedPreferences("QRCodeScannerPrefs", MODE_PRIVATE)
        val savedUser = prefs.getString("savedUser", "") ?: ""
        val savedPass = prefs.getString("savedPass", "") ?: ""
        val savedRemember = prefs.getBoolean("savedRemember", false)
        // Si recordar está activo y hay usuario y pass, intenta login automático
        if (savedRemember && savedUser.isNotBlank()) {
            android.util.Log.d("LoginActivity", "Intentando login automático con usuario guardado: $savedUser")
            ApiClient.apiService.loginPlano(nombreUsuario = savedUser, pass = savedPass).enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
                override fun onResponse(
                    call: retrofit2.Call<okhttp3.ResponseBody>,
                    response: retrofit2.Response<okhttp3.ResponseBody>
                ) {
                    val rawBody = response.body()?.string()
                    if (response.isSuccessful && rawBody != null) {
                        prefs.edit().putString("token", rawBody).apply()
                        android.util.Log.d("LoginActivity", "Token guardado en SharedPreferences: $rawBody")
                        runOnUiThread {
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        android.util.Log.e("LoginActivity", "Login automático fallido: respuesta no exitosa o token nulo")
                        // Si falla el login automático, mostrar pantalla de login
                        mostrarPantallaLogin(savedUser, savedPass, savedRemember)
                    }
                }
                override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                    android.util.Log.e("LoginActivity", "loginPlano onFailure: ${t.message}", t)
                    // Si falla el login automático, mostrar pantalla de login
                    mostrarPantallaLogin(savedUser, savedPass, savedRemember)
                }
            })
        } else {
            mostrarPantallaLogin(savedUser, savedPass, savedRemember)
        }
    }

    private fun mostrarPantallaLogin(savedUser: String, savedPass: String, savedRemember: Boolean) {
        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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
                        onLogin = { nombreUsuario, pass, recordar ->
                            android.util.Log.d("LoginActivity", "Login button clicked with user: $nombreUsuario, pass: $pass, recordar: $recordar")
                            if (nombreUsuario.isBlank()) {
                                android.util.Log.e("LoginActivity", "Usuario vacío, no se llama a la API")
                                return@LoginScreen
                            }
                            val prefs = this@LoginActivity.getSharedPreferences("QRCodeScannerPrefs", MODE_PRIVATE)
                            ApiClient.apiService.loginPlano(nombreUsuario = nombreUsuario, pass = pass).enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
                                override fun onResponse(
                                    call: retrofit2.Call<okhttp3.ResponseBody>,
                                    response: retrofit2.Response<okhttp3.ResponseBody>
                                ) {
                                    val rawBody = response.body()?.string()
                                    if (response.isSuccessful && rawBody != null) {
                                        prefs.edit().putString("token", rawBody).apply()
                                        if (recordar) {
                                            prefs.edit().putString("savedUser", nombreUsuario)
                                                .putString("savedPass", pass)
                                                .putBoolean("savedRemember", true)
                                                .apply()
                                        } else {
                                            prefs.edit().remove("savedUser").remove("savedPass").putBoolean("savedRemember", false).apply()
                                        }
                                        android.util.Log.d("LoginActivity", "Token guardado en SharedPreferences: $rawBody")
                                        runOnUiThread {
                                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        }
                                    }
                                }
                                override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                                    android.util.Log.e("LoginActivity", "loginPlano onFailure: ${t.message}", t)
                                }
                            })
                        },
                        savedUser = savedUser,
                        savedPass = savedPass,
                        savedRemember = savedRemember
                    )
                }
            }
        }
    }
}