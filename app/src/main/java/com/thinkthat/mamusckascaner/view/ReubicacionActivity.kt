package com.thinkthat.mamusckascaner.view

import ReubicacionScreen
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme
import com.thinkthat.mamusckascaner.database.DatabaseHelper
import com.thinkthat.mamusckascaner.database.ReubicacionEntity
import com.thinkthat.mamusckascaner.utils.AppLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReubicacionActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        dbHelper = DatabaseHelper(this)
        
        // Verificar si hay reubicaciones pendientes
        val reubicacionesPendientes = dbHelper.getReubicacionesPendientes()
        val showListScreen = reubicacionesPendientes.isNotEmpty()
        
        Log.d("DEBUG_REUBICACION", "onCreate - Pendientes encontradas: ${reubicacionesPendientes.size}")
        Log.d("DEBUG_REUBICACION", "onCreate - Mostrar lista: $showListScreen")
        
        // Si viene una reubicación a retomar desde el intent
        val retomar = intent.getBooleanExtra("retomar", false)
        val partidaRetomar = intent.getStringExtra("partida")
        val ubicacionOrigenRetomar = intent.getStringExtra("ubicacionOrigen")
        val ubicacionDestinoRetomar = intent.getStringExtra("ubicacionDestino")
        val idRetomar = intent.getLongExtra("id", -1L)
        
        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var mostrarLista by remember { mutableStateOf(showListScreen && !retomar) }
                    var producto by remember { mutableStateOf(partidaRetomar) }
                    var ubicacionOrigen by remember { mutableStateOf(ubicacionOrigenRetomar) }
                    var ubicacionDestino by remember { mutableStateOf(ubicacionDestinoRetomar) }
                    var tipoScan by remember { mutableStateOf<String?>(null) }
                    var idReubicacionActual by remember { mutableStateOf(idRetomar) }
                    
                    // Guardado automático cuando cambien los valores (al menos un campo)
                    LaunchedEffect(producto, ubicacionOrigen, ubicacionDestino) {
                        if (!producto.isNullOrBlank() || 
                            !ubicacionOrigen.isNullOrBlank() || 
                            !ubicacionDestino.isNullOrBlank()) {
                            try {
                                val prefs = getSharedPreferences("QRCodeScannerPrefs", MODE_PRIVATE)
                                val codDeposito = prefs.getString("savedDeposito", "") ?: ""
                                
                                val fechaCreacion = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date())
                                
                                val reubicacion = ReubicacionEntity(
                                    id = if (idReubicacionActual > 0) idReubicacionActual else 0,
                                    partida = producto ?: "",
                                    ubicacionOrigen = ubicacionOrigen ?: "",
                                    ubicacionDestino = ubicacionDestino ?: "",
                                    codDeposito = codDeposito,
                                    fechaCreacion = fechaCreacion,
                                    estado = "pendiente"
                                )
                                
                                if (idReubicacionActual > 0) {
                                    // Actualizar existente
                                    dbHelper.updateReubicacion(reubicacion)
                                    Log.d("DEBUG_REUBICACION", "Actualizando reubicación ID: ${idReubicacionActual}")
                                } else {
                                    // Insertar nueva y guardar el ID
                                    val id = dbHelper.insertReubicacion(reubicacion)
                                    idReubicacionActual = id
                                    Log.d("DEBUG_REUBICACION", "Reubicación guardada automáticamente con ID: $id")
                                }
                            } catch (e: Exception) {
                                Log.e("DEBUG_REUBICACION", "Error al guardar automáticamente", e)
                            }
                        }
                    }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val value = result.data?.getStringExtra("scanResult")
                            Log.d("DEBUG_REUBICACION", "Scanner result - tipo: $tipoScan, value: '$value'")
                            when (tipoScan) {
                                "producto" -> producto = value
                                "ubicacion_origen" -> ubicacionOrigen = value
                                "ubicacion_destino" -> ubicacionDestino = value
                            }
                            Log.d("DEBUG_REUBICACION", "Después de asignar - producto: '$producto', ubicacionOrigen: '$ubicacionOrigen', ubicacionDestino: '$ubicacionDestino'")
                        }
                    }

                    if (mostrarLista) {
                        ReubicacionListScreen(
                            onBack = { finish() },
                            onNewReubicacion = { mostrarLista = false },
                            onResumeReubicacion = { reubicacion ->
                                producto = reubicacion.partida
                                ubicacionOrigen = reubicacion.ubicacionOrigen
                                ubicacionDestino = reubicacion.ubicacionDestino
                                idReubicacionActual = reubicacion.id
                                mostrarLista = false
                            }
                        )
                    } else {
                        ReubicacionScreen(
                            onBack = {
                                // El guardado ya se hizo automáticamente
                                Log.d("DEBUG_REUBICACION", "onBack - cerrando Activity")
                                finish()
                            },
                            onReubicarClick = { tipo ->
                                tipoScan = tipo
                                val intent = Intent(this, BarcodeScannerActivity::class.java)
                                intent.putExtra("modo", tipo)
                                scannerLauncher.launch(intent)
                            },
                            producto = producto,
                            ubicacionOrigen = ubicacionOrigen,
                            ubicacionDestino = ubicacionDestino,
                            idReubicacionActual = idReubicacionActual,
                            dbHelper = dbHelper,
                            onProductoChange = { newValue -> producto = newValue },
                            onUbicacionOrigenChange = { newValue -> ubicacionOrigen = newValue },
                            onUbicacionDestinoChange = { newValue -> ubicacionDestino = newValue }
                        )
                    }
                }
            }
        }
    }
}