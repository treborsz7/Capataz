package com.thinkthat.mamusckascaner.view

import EstivacionScreen
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
import com.thinkthat.mamusckascaner.service.Services.ApiClient
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme
import com.thinkthat.mamusckascaner.utils.AppLogger
import com.thinkthat.mamusckascaner.database.DatabaseHelper
import com.thinkthat.mamusckascaner.database.EstivacionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EstivacionActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        dbHelper = DatabaseHelper(this)
        
        // Verificar si hay estivaciones pendientes
        val estivacionesPendientes = dbHelper.getEstivacionesPendientes()
        val showListScreen = estivacionesPendientes.isNotEmpty()
        
        Log.d("DEBUG_ESTIVACION", "onCreate - Pendientes encontradas: ${estivacionesPendientes.size}")
        Log.d("DEBUG_ESTIVACION", "onCreate - Mostrar lista: $showListScreen")
        
        // Si viene una estivación a retomar desde el intent
        val retomar = intent.getBooleanExtra("retomar", false)
        val partidaRetomar = intent.getStringExtra("partida")
        val ubicacionRetomar = intent.getStringExtra("ubicacion")
        val idRetomar = intent.getLongExtra("id", -1L)
        
        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.White) {
                    var mostrarLista by remember { mutableStateOf(showListScreen && !retomar) }
                    var producto by rememberSaveable { mutableStateOf(partidaRetomar) }
                    var ubicacion by rememberSaveable { mutableStateOf(ubicacionRetomar) }
                    var tipoScan by rememberSaveable { mutableStateOf<String?>(null) }
                    var idEstivacionActual by remember { mutableStateOf(idRetomar) }
                    
                    // Guardado automático cuando cambien los valores (al menos un campo)
                    LaunchedEffect(producto, ubicacion) {
                        if (!producto.isNullOrBlank() || !ubicacion.isNullOrBlank()) {
                            try {
                                val prefs = getSharedPreferences("QRCodeScannerPrefs", MODE_PRIVATE)
                                val codDeposito = prefs.getString("savedDeposito", "") ?: ""
                                
                                val fechaCreacion = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date())
                                
                                val estivacion = EstivacionEntity(
                                    id = if (idEstivacionActual > 0) idEstivacionActual else 0,
                                    partida = producto ?: "",
                                    ubicacion = ubicacion ?: "",
                                    codDeposito = codDeposito,
                                    fechaCreacion = fechaCreacion,
                                    estado = "pendiente"
                                )
                                
                                if (idEstivacionActual > 0) {
                                    // Actualizar existente
                                    dbHelper.updateEstivacion(estivacion)
                                    Log.d("DEBUG_ESTIVACION", "Actualizando estivación ID: ${idEstivacionActual}")
                                } else {
                                    // Insertar nueva y guardar el ID
                                    val id = dbHelper.insertEstivacion(estivacion)
                                    idEstivacionActual = id
                                    Log.d("DEBUG_ESTIVACION", "Estivación guardada automáticamente con ID: $id")
                                }
                            } catch (e: Exception) {
                                Log.e("DEBUG_ESTIVACION", "Error al guardar automáticamente", e)
                            }
                        }
                    }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            val value = result.data?.getStringExtra("scanResult")
                            Log.d("DEBUG_ESTIVACION", "Scanner result - tipo: $tipoScan, value: '$value'")
                            when (tipoScan) {
                                "producto" -> producto = value
                                "ubicacion" -> ubicacion = value
                            }
                            Log.d("DEBUG_ESTIVACION", "Después de asignar - producto: '$producto', ubicacion: '$ubicacion'")
                            if(tipoScan == "producto")
                            {
                                ApiClient.apiService.ubicacionesParaEstibar(codArticu = producto, codDeposi = "3B", optimizaRecorrido= true)

                                    .enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
                                    override fun onResponse(
                                        call: retrofit2.Call<okhttp3.ResponseBody>,
                                        response: retrofit2.Response<okhttp3.ResponseBody>
                                    ) {
                                        val rawBody = response.body()?.string()
                                        if (response.isSuccessful && rawBody != null) {
                                           // prefs.edit().putString("token", rawBody).apply()
                                            android.util.Log.d("EstivacionActivity", "Body: $rawBody")
                                            /*runOnUiThread {
                                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                                finish()
                                            }*/
                                        } else {
                                            AppLogger.logError(
                                                tag = "EstivacionActivity",
                                                message = "ubicacion fallida: respuesta no exitosa"
                                            )
                                            // Si falla el login automático, mostrar pantalla de login
                                            //mostrarPantallaLogin(savedUser, savedPass, savedRemember)
                                        }
                                    }
                                    override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                                        AppLogger.logError(
                                            tag = "EstivacionActivity",
                                            message = "ubicacion onFailure: ${t.message}",
                                            throwable = t
                                        )
                                        // Si falla el login automático, mostrar pantalla de login
                                        //mostrarPantallaLogin(savedUser, savedPass, savedRemember)
                                    }
                                })

                            }
                        }
                    }

                    if (mostrarLista) {
                        EstivacionListScreen(
                            onBack = { finish() },
                            onNewEstivacion = { mostrarLista = false },
                            onResumeEstivacion = { estivacion ->
                                producto = estivacion.partida
                                ubicacion = estivacion.ubicacion
                                idEstivacionActual = estivacion.id
                                mostrarLista = false
                            }
                        )
                    } else {
                        EstivacionScreen(
                            onBack = {
                                // El guardado ya se hizo automáticamente
                                Log.d("DEBUG_ESTIVACION", "onBack - cerrando Activity")
                                finish()
                            },
                            onStockearClick = { tipo ->
                                Log.d("EstivacionActivity", "Tipo de escaneo: $tipo")
                                tipoScan = tipo
                                val intent = Intent(this, BarcodeScannerActivity::class.java)
                                intent.putExtra("modo", tipo)
                                scannerLauncher.launch(intent)
                            },
                            producto = producto,
                            ubicacion = ubicacion,
                            idEstivacionActual = idEstivacionActual,
                            dbHelper = dbHelper,
                            onProductoChange = { newValue -> producto = newValue },
                            onUbicacionChange = { newValue -> ubicacion = newValue }
                        )
                    }
                }
            }
        }
    }
}