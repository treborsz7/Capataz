package com.thinkthat.mamusckascaner.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import com.thinkthat.mamusckascaner.service.Services.ApiClient
import com.thinkthat.mamusckascaner.service.Services.OrdenLanzada
import com.thinkthat.mamusckascaner.service.Services.OrdenTrabajoCompleta
import com.thinkthat.mamusckascaner.service.Services.UbicacionResponse
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme
import kotlinx.serialization.json.buildJsonObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.thinkthat.mamusckascaner.utils.AppLogger
import com.thinkthat.mamusckascaner.utils.QRData
import com.thinkthat.mamusckascaner.utils.parseQRData

class RecolectarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Obtener datos del QR y orden ID desde los extras
                    val qrData = intent.getStringExtra("qrData")
                    val fromQR = intent.getBooleanExtra("fromQR", false)
                    val optimizaRecorrido = intent.getBooleanExtra("optimizaRecorrido", true) // Por defecto true
                    val ordenId = if (fromQR && qrData != null) {
                        // Parsear el QR para obtener el ID del pedido
                        parseQRData(qrData).pedido.toIntOrNull() ?: -1
                    } else {
                        intent.getIntExtra("ordenId", -1)
                    }
                    
                    // Only track variables needed for ubicaciones
                    var producto by rememberSaveable { mutableStateOf<String?>(null) }
                    var ubicacion by rememberSaveable { mutableStateOf<String?>(null) }
                    var tipoScan by rememberSaveable { mutableStateOf<String?>(null) }
                    var isLoadingUbicaciones by remember { mutableStateOf(false) }
                    var errorUbicaciones by remember { mutableStateOf<String?>(null) }
                    var ubicacionesRecolectar by remember { mutableStateOf<String?>(null) }

                    // Función para cargar ubicaciones
                    val cargarUbicaciones = {
                        if (ordenId != -1) {
                            Log.d("RecolectarActivity", "Iniciando carga de ubicaciones con idPed: $ordenId")
                            isLoadingUbicaciones = true
                            errorUbicaciones = null

                            ApiClient.apiService.UbicacionesParaRecolectar(
                                idPed = ordenId,
                                optimizaRecorrido = optimizaRecorrido
                            ).enqueue(object : Callback<List<UbicacionResponse>> {
                                override fun onResponse(
                                    call: Call<List<UbicacionResponse>>,
                                    response: Response<List<UbicacionResponse>>
                                ) {
                                    if (response.isSuccessful) {
                                        isLoadingUbicaciones = false
                                        val lista = response.body().orEmpty()
                                        val raw = Gson().toJson(lista)
                                        ubicacionesRecolectar = raw
                                        Log.d("RecolectarActivity", "Ubicaciones OK. Cantidad: ${lista.size}\nRAW: $raw")
                                    } else {
                                        val code = response.code()
                                        val err = response.errorBody()?.string()
                                        Log.w("RecolectarActivity", "Ubicaciones falló: code=${code} msg=${response.message()} body=${err}")
                                        // Retry with path param if 404 or generic Not Found
                                        if (code == 404 || response.message().contains("Not Found", true)) {
                                            Log.d("RecolectarActivity", "Reintentando Ubicaciones con path ...")
                                            isLoadingUbicaciones = false
                                            AppLogger.logError(
                                                tag = "RecolectarActivity",
                                                message = "Ubicaciones no encontradas: code=$code message=${response.message()} body=${err ?: "sin cuerpo"}"
                                            )
                                            errorUbicaciones = "No se encontraron ubicaciones para el pedido."
                                        } else {
                                            isLoadingUbicaciones = false
                                            AppLogger.logError(
                                                tag = "RecolectarActivity",
                                                message = "Ubicaciones error: code=$code message=${response.message()} body=${err ?: "sin cuerpo"}"
                                            )
                                            errorUbicaciones = "No se pudieron cargar las ubicaciones. Intenta nuevamente."
                                        }
                                    }
                                }

                                override fun onFailure(call: Call<List<UbicacionResponse>>, t: Throwable) {
                                    isLoadingUbicaciones = false
                                    AppLogger.logError(
                                        tag = "RecolectarActivity",
                                        message = "Fallo ubicaciones: ${t.message}",
                                        throwable = t
                                    )
                                    errorUbicaciones = "No se pudieron cargar las ubicaciones. Verifica tu conexión."
                                }
                            })
                        }
                    }

                    // Cargar directamente las ubicaciones cuando tenemos el ID del pedido
                    LaunchedEffect(ordenId) {
                        cargarUbicaciones()
                    }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            val value = result.data?.getStringExtra("scanResult")
                            when (tipoScan) {
                                "producto" -> producto = value
                                "ubicacion" -> ubicacion = value
                            }
                        }
                    }

                    RecolectarScreen(
                        onBack = { finish() },
                        onClose = { 
                            // Navigate to MainActivity (main screen)
                            val intent = Intent(this@RecolectarActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        },
                        onStockearClick = { tipo ->
                            Log.d("RecolectarActivity", "Tipo de escaneo: $tipo")
                            tipoScan = tipo
                            val intent = Intent(this, BarcodeScannerActivity::class.java)
                            intent.putExtra("modo", tipo)
                            scannerLauncher.launch(intent)
                        },
                        producto = producto,
                        ubicacion = ubicacion,
                        ordenCompleta = null, // No longer loading complete order
                        isLoadingOrden = false, // No longer loading order
                        errorOrden = null, // No order errors
                        ubicacionesRecolectar = ubicacionesRecolectar,
                        isLoadingUbicaciones = isLoadingUbicaciones,
                        errorUbicaciones = errorUbicaciones,
                        qrData = if (fromQR && qrData != null) parseQRData(qrData) else null,
                        fromQR = fromQR,
                        onRetryUbicaciones = { cargarUbicaciones() },
                        onSuccess = {
                            // Navigate to MainActivity (main screen) when success
                            val intent = Intent(this@RecolectarActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        },
                        onClearScanValues = {
                            // Limpiar valores escaneados después de ser asignados
                            Log.d("RecolectarActivity", "Limpiando valores escaneados: producto=$producto, ubicacion=$ubicacion")
                            producto = null
                            ubicacion = null
                        }
                    )
                }
            }
        }
    }
}
