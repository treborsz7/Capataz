package com.thinkthat.mamusckascaner.view
import BarcodeScannerScreen
import android.content.Intent
import com.thinkthat.mamusckascaner.model.OperationType
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.key
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme
import com.thinkthat.mamusckascaner.viewmodel.BarCodeScannerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BarCodeScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarCodeScannerTheme {
               Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    var showEstivacion by remember { mutableStateOf(false) }
                    var showScanner by remember { mutableStateOf(false) }
                    var scanResult by remember { mutableStateOf<String?>(null) }
                    var navigationDirection by remember { mutableStateOf(1) } // 1: adelante, -1: atrás
                    var scannerKey by remember { mutableStateOf(0) }

                    // Manejar el botón físico/gesto de back
                    BackHandler(enabled = showEstivacion || showScanner) {
                        when {
                            showScanner -> {
                                showScanner = false
                                navigationDirection = -1
                            }
                            showEstivacion -> {
                                showEstivacion = false
                                navigationDirection = -1
                            }
                        }
                    }

                    Box(Modifier.fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues()),
                    ) {
                        // MainScreen
                        AnimatedVisibility(
                            visible = !showEstivacion && !showScanner,
                            enter = slideInHorizontally(
                                initialOffsetX = { navigationDirection * it },
                                animationSpec = tween(durationMillis = 500)
                            ) + fadeIn(animationSpec = tween(500)),
                            exit = slideOutHorizontally(
                                targetOffsetX = { -navigationDirection * it },
                                animationSpec = tween(durationMillis = 500)
                            ) + fadeOut(animationSpec = tween(500))
                        ) {                            MainScreen(
                                onScanRequest = { operationType ->
                                    when (operationType) {
                                        OperationType.ESTIVAR -> {
                                            startActivity(Intent(this@MainActivity, EstivacionActivity::class.java))
                                        }
                                        OperationType.PICKEAR -> {
                                            // TODO: Implementar la lógica para pickear
                                            showScanner = true
                                            navigationDirection = 1
                                        }
                                        OperationType.RECOLECTAR -> {
                                            // Navegar a ListadoOrdenesActivity
                                            val intent = Intent(this@MainActivity, ListadoOrdenesActivity::class.java)
                                            startActivity(intent)
                                        }
                                    }
                                },
                                onLogout = {
                                    try {
                                        android.util.Log.d("MainActivity", "Logout button pressed")
                                        // Limpiar credenciales guardadas
                                        val prefs = getSharedPreferences("QRCodeScannerPrefs", MODE_PRIVATE)
                                        prefs.edit()
                                            .remove("savedUser")
                                            .remove("savedPass")
                                            .remove("token")
                                            .remove("savedEmpresa")
                                            .remove("savedDeposito")
                                            .putBoolean("savedRemember", false)
                                            .apply()
                                        
                                        android.util.Log.d("MainActivity", "Credentials and deposito cleared, navigating to LoginActivity")
                                        // Navegar al LoginActivity
                                        val intent = Intent(this@MainActivity,LoginActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        android.util.Log.d("MainActivity", "LoginActivity started")
                                        // No llamar finish() para ver si eso es el problema
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainActivity", "Error during logout: ${e.message}", e)
                                    }
                                }
                            )
                        }
                        // EstivacionScreen con botón de volver
                        AnimatedVisibility(
                            visible = showEstivacion && !showScanner,
                            enter = slideInHorizontally(
                                initialOffsetX = { navigationDirection * it },
                                animationSpec = tween(durationMillis = 500)
                            ) + fadeIn(animationSpec = tween(500)),
                            exit = slideOutHorizontally(
                                targetOffsetX = { -navigationDirection * it },
                                animationSpec = tween(durationMillis = 500)
                            ) + fadeOut(animationSpec = tween(500))
                        ) {

                        }
                        // BarcodeScannerScreen
                        if (showScanner) {
                            key(scannerKey) {
                                BarcodeScannerScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        showScanner = false
                                        navigationDirection = -1
                                    },
                                    onScanResult = { result ->
                                        scanResult = result
                                        showScanner = false
                                        navigationDirection = -1
                                    },
                                    scannerKey = scannerKey
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
