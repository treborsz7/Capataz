package com.codegalaxy.barcodescanner.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.codegalaxy.barcodescanner.ui.theme.BarCodeScannerTheme
import androidx.compose.foundation.layout.fillMaxSize
import com.codegalaxy.barcodescanner.model.OrdenTrabajo

class ListadoOrdenesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarCodeScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    ListadoOrdenesScreen(
                        onBack = { finish() },
                        onTomaOrden = { orden ->
                            val intent = Intent(this, RecolectarActivity::class.java)
                            intent.putExtra("ordenTrabajo", orden)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}
