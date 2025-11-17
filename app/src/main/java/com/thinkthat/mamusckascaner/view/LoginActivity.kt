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
import com.thinkthat.mamusckascaner.utils.AppLogger

class LoginActivity : ComponentActivity() {
    // Valores fijos para empresa y depósito
    private val EMPRESA_FIJA = "3"
    private val DEPOSITO_FIJO = "2B"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Mantener el splash visible por 4 segundos
        Thread.sleep(2000)
        
        // Cambiar del tema Splash al tema normal antes de setContent
        setTheme(com.thinkthat.mamusckascaner.R.style.Theme_BarCodeScanner)
        
        AppLogger.init(applicationContext)
        ApiClient.init(applicationContext)
        enableEdgeToEdge()
        android.util.Log.d("LoginActivity", "LoginActivity is being created")
        val prefs = getSharedPreferences("QRCodeScannerPrefs", MODE_PRIVATE)
        val savedUser = prefs.getString("savedUser", "") ?: ""
        val savedPass = prefs.getString("savedPass", "") ?: ""
        val savedRemember = prefs.getBoolean("savedRemember", false)
        
        // Asegurar que empresa y depósito siempre estén guardados
        prefs.edit()
            .putString("savedEmpresa", EMPRESA_FIJA)
            .putString("savedDeposito", DEPOSITO_FIJO)
            .apply()
        
        // Auto-login si recordar está activado y hay credenciales
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
                            .apply()
                        
                        // Ir directo a MainActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        AppLogger.logError(
                            tag = "LoginActivity",
                            message = "LoginPlano falló en auto login: code=${response.code()} message=${response.message()}"
                        )
                        mostrarPantallaLogin(savedUser, savedPass, savedRemember, prefs)
                    }
                }
                override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                    AppLogger.logError(
                        tag = "LoginActivity",
                        message = "LoginPlano onFailure en auto login: ${t.message}",
                        throwable = t
                    )
                    mostrarPantallaLogin(savedUser, savedPass, savedRemember, prefs)
                }
            })
        } else {
            mostrarPantallaLogin(savedUser, savedPass, savedRemember, prefs)
        }
    }

    private fun mostrarPantallaLogin(
        savedUser: String,
        savedPass: String,
        savedRemember: Boolean,
        prefs: SharedPreferences
    ) {
        setContent {
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            
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
                        onLogin = { nombreUsuario, pass, recordar ->
                            isLoading = true
                            errorMessage = null
                            
                            if (nombreUsuario.isBlank()) {
                                errorMessage = "El usuario no puede estar vacío."
                                isLoading = false
                                return@LoginScreen
                            }
                            
                            ApiClient.apiService.loginPlano(nombreUsuario = nombreUsuario, pass = pass).enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
                                override fun onResponse(
                                    call: retrofit2.Call<okhttp3.ResponseBody>,
                                    response: retrofit2.Response<okhttp3.ResponseBody>
                                ) {
                                    val rawBody = response.body()?.string()
                                    if (response.isSuccessful && rawBody != null) {
                                        val editor = prefs.edit()
                                            .putString("token", rawBody)
                                            .putString("savedEmpresa", EMPRESA_FIJA)
                                            .putString("savedDeposito", DEPOSITO_FIJO)
                                            
                                        if (recordar) {
                                            // Guardar credenciales si recordar está activado
                                            editor
                                                .putString("savedUser", nombreUsuario)
                                                .putString("savedPass", pass)
                                                .putBoolean("savedRemember", true)
                                        } else {
                                            // Solo limpiar usuario, contraseña y recordar
                                            editor
                                                .remove("savedUser")
                                                .remove("savedPass")
                                                .putBoolean("savedRemember", false)
                                        }
                                        editor.apply()
                                        
                                        isLoading = false
                                        // Ir directo a MainActivity
                                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        isLoading = false
                                        AppLogger.logError(
                                            tag = "LoginActivity",
                                            message = "LoginPlano falló: code=${response.code()} message=${response.message()}"
                                        )
                                        errorMessage = "No se pudo iniciar sesión. Revisa tus credenciales."
                                    }
                                }
                                override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                                    isLoading = false
                                    AppLogger.logError(
                                        tag = "LoginActivity",
                                        message = "LoginPlano onFailure: ${t.message}",
                                        throwable = t
                                    )
                                    errorMessage = "No se pudo iniciar sesión por un problema de conexión."
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