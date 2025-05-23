package com.codegalaxy.barcodescanner.view

import EstivacionScreen
import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    var producto by rememberSaveable { mutableStateOf<String?>(null) }
                    var ubicacion by rememberSaveable { mutableStateOf<String?>(null) }
                    var tipoScan by rememberSaveable { mutableStateOf<String?>(null) }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val value = result.data?.getStringExtra("scanResult")
                            when (tipoScan) {
                                "producto" -> producto = value
                                "ubicacion" -> ubicacion = value
                            }
                        }
                    }

                    EstivacionScreen(
                        onBack = { finish() },
                        onStockearClick = { tipo ->
                            tipoScan = tipo
                            scannerLauncher.launch(Intent(this, BarcodeScannerActivity::class.java))
                        },
                        producto = producto,
                        ubicacion = ubicacion
                    )

                    // Si quieres mostrar ambos resultados:
                    // Puedes agregar un Composable aquí para mostrar producto y ubicacion
                }
            }
        }
    }
}

