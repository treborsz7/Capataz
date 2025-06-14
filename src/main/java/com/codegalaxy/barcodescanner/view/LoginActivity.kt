// src/main/java/com/codegalaxy/barcodescanner/view/LoginActivity.kt
package com.codegalaxy.barcodescanner.view

import ApiService
import LoginEmpresaResponse
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
            val savedEmpresa = prefs.getString("savedEmpresa", "31") ?: "31"
            android.util.Log.d("LoginActivity", "Intentando login automático con usuario guardado: $savedUser")
            ApiClient.apiService.loginPlano(nombreUsuario = savedUser, pass = savedPass).enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
                override fun onResponse(
                    call: retrofit2.Call<okhttp3.ResponseBody>,
                    response: retrofit2.Response<okhttp3.ResponseBody>
                ) {
                    val rawBody = response.body()?.string()
                    android.util.Log.d("token", rawBody.toString())

                    if (response.isSuccessful && rawBody != null) {
                        prefs.edit().putString("token", rawBody).apply()
                        TokenProvider.token = rawBody.replace("\"", "")
                        android.util.Log.d("LoginActivity", "Token guardado en SharedPreferences y TokenProvider: $rawBody")
                        // Llamar a loginEmpresa con el token como Bearer
                        val token = rawBody.replace("\"", "")
                        android.util.Log.d("token", token)


//                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
//                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                        startActivity(intent)
//                        finish()


                        // El header Authorization ya lo agrega el interceptor, así que aquí puedes pasar una cadena vacía o el token, pero el interceptor lo pondrá correctamente
                        android.util.Log.d("LoginActivity", "Llamando a loginEmpresa con interceptor global para token")
                        ApiClient.apiService.loginEmpresa(
                            idEmpresa = savedEmpresa,
                            authorization = ""
                        ).enqueue(object : retrofit2.Callback<LoginEmpresaResponse> {
                            override fun onResponse(
                                call: retrofit2.Call<LoginEmpresaResponse>,
                                response: retrofit2.Response<LoginEmpresaResponse>
                            ) {
                                android.util.Log.d("LoginActivity", "loginEmpresa onResponse: isSuccessful=${response.isSuccessful}, code=${response.code()}, body=${response.body()}, errorBody=${response.errorBody()?.string()}")
                                if (response.isSuccessful) {
                                    android.util.Log.d("LoginActivity", "loginEmpresa exitoso")
                                    runOnUiThread {
                                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    }
                                } else {
                                    android.util.Log.e("LoginActivity", "loginEmpresa fallido: ${response.errorBody()?.string()} | code: ${response.code()} | headers: ${response.headers()} | request: ${call.request()}")
                                    mostrarPantallaLogin(savedUser, savedPass, savedRemember)
                                }
                            }
                            override fun onFailure(call: retrofit2.Call<LoginEmpresaResponse>, t: Throwable) {
                                android.util.Log.e("LoginActivity", "loginEmpresa onFailure: ${t.message}", t)
                                mostrarPantallaLogin(savedUser, savedPass, savedRemember)
                            }
                        })
                    } else {
                        android.util.Log.e("LoginActivity", "Login automático fallido: respuesta no exitosa o token nulo")
                        mostrarPantallaLogin(savedUser, savedPass, savedRemember)
                    }
                }
                override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                    android.util.Log.e("LoginActivity", "loginPlano onFailure: ${t.message}", t)
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
                        onLogin = { nombreUsuario, pass, recordar, empresa ->
                            android.util.Log.d("LoginActivity", "Login button clicked with user: $nombreUsuario, pass: $pass, recordar: $recordar, empresa: $empresa")
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
                                        TokenProvider.token = rawBody.replace("\"", "")
                                        if (recordar) {
                                            prefs.edit().putString("savedUser", nombreUsuario)
                                                .putString("savedPass", pass)
                                                .putBoolean("savedRemember", true)
                                                .putString("savedEmpresa", empresa)
                                                .apply()
                                        } else {
                                            prefs.edit().remove("savedUser").remove("savedPass").remove("savedEmpresa").putBoolean("savedRemember", false).apply()
                                        }
                                        android.util.Log.d("LoginActivity", "Token guardado en SharedPreferences y TokenProvider: $rawBody")
                                        // Llamar a loginEmpresa con el token como Bearer
                                        val token = rawBody.replace("\"", "")
                                        android.util.Log.e("token", token)

                                        // El header Authorization ya lo agrega el interceptor, así que aquí puedes pasar una cadena vacía
                                        android.util.Log.d("LoginActivity", "Llamando a loginEmpresa con interceptor global para token")
                                        ApiClient.apiService.loginEmpresa(
                                            idEmpresa = empresa,
                                            authorization = ""
                                        ).enqueue(object : retrofit2.Callback<LoginEmpresaResponse> {
                                            override fun onResponse(
                                                call: retrofit2.Call<LoginEmpresaResponse>,
                                                response: retrofit2.Response<LoginEmpresaResponse>
                                            ) {
                                                android.util.Log.d("LoginActivity", "loginEmpresa onResponse: isSuccessful=${response.isSuccessful}, code=${response.code()}, body=${response.body()}, errorBody=${response.errorBody()?.string()}")

                                                if (response.isSuccessful) {
                                                    android.util.Log.d("LoginActivity", "loginEmpresa exitoso")
                                                    runOnUiThread {
                                                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        startActivity(intent)
                                                        finish()
                                                    }
                                                } else {
                                                    android.util.Log.e("LoginActivity", "loginEmpresa fallido: ${response.errorBody()?.string()}")
                                                }
                                            }
                                            override fun onFailure(call: retrofit2.Call<LoginEmpresaResponse>, t: Throwable) {
                                                android.util.Log.e("LoginActivity", "loginEmpresa onFailure: ${t.message}", t)
                                            }
                                        })
                                    }
                                }
                                override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                                    android.util.Log.e("LoginActivity", "loginPlano onFailure: ${t.message}", t)
                                }
                            })
                        },
                        savedUser = savedUser,
                        savedPass = savedPass,
                        savedRemember = savedRemember,
                        savedEmpresa = getSharedPreferences("QRCodeScannerPrefs", MODE_PRIVATE).getString("savedEmpresa", "31") ?: "31"
                    )
                }
            }
        }
    }
}