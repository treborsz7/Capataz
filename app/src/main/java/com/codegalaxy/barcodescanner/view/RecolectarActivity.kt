package com.codegalaxy.barcodescanner.view

import RecolectarScreen
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

class RecolectarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val ordenTrabajo = intent.getSerializableExtra("ordenTrabajo") as? com.codegalaxy.barcodescanner.model.OrdenTrabajo
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
                        }
                    }

                    RecolectarScreen(
                        onBack = { finish() },
                        onStockearClick = { tipo ->
                            Log.e("tIPO", tipo)
                            tipoScan = tipo
                            val intent = Intent(this, BarcodeScannerActivity::class.java)
                            intent.putExtra("modo", tipo)
                            scannerLauncher.launch(intent)
                        },
                        producto = producto,
                        ubicacion = ubicacion,
                        ordenTrabajo = ordenTrabajo
                    )
                }
            }
        }
    }
}
