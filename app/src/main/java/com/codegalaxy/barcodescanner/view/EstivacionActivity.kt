package com.codegalaxy.barcodescanner.view

import ApiClient
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
import com.codegalaxy.barcodescanner.ui.theme.BarCodeScannerTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

class EstivacionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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
                                ApiClient.apiService.ubicacionesParaEstibar(codArticu = producto, idEmp= 17, codDeposi = "01", optimizaRecorrido= true)
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
                                            android.util.Log.e("EstivacionActivity", "ubicacion fallida: respuesta no exitosa")
                                            // Si falla el login automático, mostrar pantalla de login
                                            //mostrarPantallaLogin(savedUser, savedPass, savedRemember)
                                        }
                                    }
                                    override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                                        android.util.Log.e("EstivacionActivity", "ubicacion onFailure: ${t.message}", t)
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
                            Log.e("tIPO", tipo)
                            tipoScan = tipo
                            scannerLauncher.launch(Intent(this, BarcodeScannerActivity::class.java))
                        },
                        producto = producto,
                        ubicacion = ubicacion
                    )
                }
            }
        }
    }
}

