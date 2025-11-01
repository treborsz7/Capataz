package com.thinkthat.mamusckascaner.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme

class RecolectarByQRActivity : ComponentActivity() {
    
    private var scannedResult by mutableStateOf<String?>(null)
    
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scannedContent = result.data?.getStringExtra("scanResult")
            scannedResult = scannedContent
        }
    }
    
    private val recolectarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Cuando regresamos de RecolectarActivity, limpiar el resultado escaneado
        // para permitir escanear de nuevo
        scannedResult = null
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RecolectarByQRScreen(
                        onBack = {
                            finish()
                        },
                        onScanQR = {
                            // Lanzar el escáner de QR para recolectar
                            val intent = Intent(this@RecolectarByQRActivity, BarcodeScannerActivity::class.java)
                            intent.putExtra("modo", "orden")
                            qrScannerLauncher.launch(intent)
                        },
                        scannedResult = scannedResult,
                        onProceedWithOrder = { qrCode, optimizaRecorrido ->
                            // Navegar a RecolectarActivity con el deposito y ID del pedido escaneado
                            val intent = Intent(this@RecolectarByQRActivity, RecolectarActivity::class.java)
                            intent.putExtra("qrData", qrCode) // Pass the raw QR data
                            intent.putExtra("fromQR", true)
                            intent.putExtra("optimizaRecorrido", optimizaRecorrido) // Pasar el valor del checkbox
                            recolectarLauncher.launch(intent)
                        }
                    )
                }
            }
        }
    }
}
