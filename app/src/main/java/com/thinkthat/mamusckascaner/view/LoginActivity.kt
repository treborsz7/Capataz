package com.thinkthat.mamusckascaner.view
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.codegalaxy.barcodescanner.view.LoginScreen
import com.thinkthat.mamusckascaner.service.Services.ApiClient
import com.thinkthat.mamusckascaner.service.Services.LoginEmpresaResponse
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme

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
        if (savedRemember && savedUser.isNotBlank() && savedPass.isNotBlank()) {
            // Llamar automáticamente a loginPlano
            ApiClient.apiService.loginPlano(nombreUsuario = savedUser, pass = savedPass).enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
                override fun onResponse(
                    call: retrofit2.Call<okhttp3.ResponseBody>,
                    response: retrofit2.Response<okhttp3.ResponseBody>
                ) {
                    val rawBody = response.body()?.string()
                    if (response.isSuccessful && rawBody != null) {
                        prefs.edit()
                            .putString("token", rawBody)
                            .putString("savedUser", savedUser)
                            .putString("savedPass", savedPass)
                            .putBoolean("savedRemember", savedRemember)
                            .putString("savedEmpresa", savedEmpresa)
                            .apply()
                        // Llamar a EmpresasGet
                        ApiClient.apiService.EmpresasGet().enqueue(object : retrofit2.Callback<List<LoginEmpresaResponse>> {
                            override fun onResponse(
                                call: retrofit2.Call<List<LoginEmpresaResponse>>,
                                response: retrofit2.Response<List<LoginEmpresaResponse>>
                            ) {
                                if (response.isSuccessful && response.body() != null) {
                                    val empresasResponse = response.body()!!
                                    // Guardar empresas y navegar si ya hay empresa seleccionada
                                    if (savedEmpresa.isNotBlank()) {
                                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        mostrarPantallaLogin(savedUser, savedPass, savedRemember, savedEmpresa,prefs)
                                    }
                                } else {
                                    mostrarPantallaLogin(savedUser, savedPass, savedRemember, savedEmpresa,prefs)
                                }
                            }
                            override fun onFailure(call: retrofit2.Call<List<LoginEmpresaResponse>>, t: Throwable) {
                                mostrarPantallaLogin(
                                    savedUser,
                                    savedPass,
                                    savedRemember,
                                    savedEmpresa,
                                    prefs
                                )
                            }
                        })
                    } else {
                        mostrarPantallaLogin(
                            savedUser,
                            savedPass,
                            savedRemember,
                            savedEmpresa,
                            prefs
                        )
                    }
                }
                override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                    mostrarPantallaLogin(savedUser, savedPass, savedRemember, savedEmpresa, prefs)
                }
            })
        } else {
            mostrarPantallaLogin(savedUser, savedPass, savedRemember, savedEmpresa, prefs)
        }
    }

    private fun mostrarPantallaLogin(
        savedUser: String,
        savedPass: String,
        savedRemember: Boolean,
        savedEmpresa: String,
        prefs: SharedPreferences
    ) {
        setContent {
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var empresas by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
            var empresaSeleccionada by remember { mutableStateOf(savedEmpresa) }
            var recordar by remember { mutableStateOf(savedRemember) }
            var deposito by remember { mutableStateOf("") }
            var prefs by remember { mutableStateOf(prefs) }
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
                        prefs = prefs,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        empresas = empresas,
                        empresaSeleccionada = empresaSeleccionada,
                        onEmpresaSeleccionada = { empresaSeleccionada = it },
                        deposito = deposito,
                        onDepositoChange = { deposito = it },
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
                                        prefs.edit()
                                            .putString("token", rawBody)
                                            .putString("savedUser", nombreUsuario)
                                            .putString("savedPass", pass)
                                            .putBoolean("savedRemember", recordar)
                                            .putString("savedEmpresa", empresaSeleccionada)
                                            .apply()
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