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
class EstivacionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.White) {
                    var producto by rememberSaveable { mutableStateOf<String?>(null) }
                    var ubicacion by rememberSaveable { mutableStateOf<String?>(null) }
                    var tipoScan by rememberSaveable { mutableStateOf<String?>(null) }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            val value = result.data?.getStringExtra("scanResult")
                            when (tipoScan) {
                                "producto" -> producto = value
                                "ubicacion" -> ubicacion = value
                            }
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

                    EstivacionScreen(
                        onBack = { finish() },
                        onStockearClick = { tipo ->
                            Log.d("EstivacionActivity", "Tipo de escaneo: $tipo")
                            tipoScan = tipo
                            val intent = Intent(this, BarcodeScannerActivity::class.java)
                            intent.putExtra("modo", tipo)
                            scannerLauncher.launch(intent)
                        },
                        producto = producto,
                        ubicacion = ubicacion
                    )
                }
            }
        }
    }
}

