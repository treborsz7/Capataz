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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.codegalaxy.barcodescanner.ui.theme.BarCodeScannerTheme

class LoginActivity : ComponentActivity() {
    private var empresasList: List<Pair<String, String>> = emptyList() // id, nombre

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(applicationContext)
        enableEdgeToEdge()
        android.util.Log.d("LoginActivity", "LoginActivity is being created")
        val prefs = getSharedPreferences("QRCodeScannerPrefs", MODE_PRIVATE)
        val savedUser = prefs.getString("savedUser", "") ?: ""
        val savedPass = prefs.getString("savedPass", "") ?: ""
        val savedRemember = prefs.getBoolean("savedRemember", false)
        val savedEmpresa = prefs.getString("savedEmpresa", "") ?: ""
        mostrarPantallaLogin(savedUser, savedPass, savedRemember, savedEmpresa)
    }

    private fun mostrarPantallaLogin(savedUser: String, savedPass: String, savedRemember: Boolean, savedEmpresa: String) {
        setContent {
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var empresas by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
            var empresaSeleccionada by remember { mutableStateOf(savedEmpresa) }

            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoginScreen(
                        onLoginSuccess = {
                            // Guardar el id de la empresa seleccionada antes de navegar
                            val prefs = getSharedPreferences("QRCodeScannerPrefs", MODE_PRIVATE)
                            prefs.edit().putString("savedEmpresa", empresaSeleccionada).apply()
                            android.util.Log.d("LoginActivity", "Login successful, navigating to MainActivity")
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        empresas = empresas,
                        empresaSeleccionada = empresaSeleccionada,
                        onEmpresaSeleccionada = { empresaSeleccionada = it },
                        onLogin = { nombreUsuario, pass, recordar, _ ->
                            isLoading = true
                            errorMessage = null
                            if (nombreUsuario.isBlank()) {
                                errorMessage = "Usuario vacío, no se llama a la API"
                                isLoading = false
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
                                        // Llamar a EmpresasGet
                                        ApiClient.apiService.EmpresasGet().enqueue(object : retrofit2.Callback<List<LoginEmpresaResponse>> {
                                            override fun onResponse(
                                                call: retrofit2.Call<List<LoginEmpresaResponse>>,
                                                response: retrofit2.Response<List<LoginEmpresaResponse>>
                                            ) {
                                                isLoading = false
                                                if (response.isSuccessful && response.body() != null) {
                                                    val empresasResponse = response.body()!!
                                                    empresas = empresasResponse.map { Pair(it.id.toString(), it.nombre) }
                                                    errorMessage = null
                                                } else {
                                                    errorMessage = "Error obteniendo empresas: ${response.message()}"
                                                }
                                            }
                                            override fun onFailure(call: retrofit2.Call<List<LoginEmpresaResponse>>, t: Throwable) {
                                                isLoading = false
                                                errorMessage = "Error de red al obtener empresas: ${t.message}"
                                            }
                                        })
                                    } else {
                                        isLoading = false
                                        errorMessage = "Login fallido: respuesta no exitosa o token nulo"
                                    }
                                }
                                override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                                    isLoading = false
                                    errorMessage = "loginPlano onFailure: ${t.message}"
                                }
                            })
                        },
                        savedUser = savedUser,
                        savedPass = savedPass,
                        savedRemember = savedRemember,
                        savedEmpresa = empresaSeleccionada
                    )
                }
            }
        }
    }
}