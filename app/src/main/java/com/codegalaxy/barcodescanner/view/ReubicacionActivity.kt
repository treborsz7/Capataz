package com.codegalaxy.barcodescanner.view

import ReubicacionScreen
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.codegalaxy.barcodescanner.ui.theme.BarCodeScannerTheme

class ReubicacionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var producto by remember { mutableStateOf<String?>(null) }
                    var ubicacionOrigen by remember { mutableStateOf<String?>(null) }
                    var ubicacionDestino by remember { mutableStateOf<String?>(null) }
                    var tipoScan by remember { mutableStateOf<String?>(null) }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val value = result.data?.getStringExtra("scanResult")
                            when (tipoScan) {
                                "producto" -> producto = value
                                "ubicacion_origen" -> ubicacionOrigen = value
                                "ubicacion_destino" -> ubicacionDestino = value
                            }
                        }
                    }

                    ReubicacionScreen(
                        onBack = { finish() },
                        onReubicarClick = { tipo ->
                            tipoScan = tipo
                            val intent = Intent(this, BarcodeScannerActivity::class.java)
                            intent.putExtra("modo", "reubicacion")
                            scannerLauncher.launch(intent)
                        },
                        producto = producto,
                        ubicacionOrigen = ubicacionOrigen,
                        ubicacionDestino = ubicacionDestino
                    )
                }
            }
        }
    }
}
