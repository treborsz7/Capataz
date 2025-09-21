package com.thinkthat.mamusckascaner.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.codegalaxy.barcodescanner.view.ListadoOrdenesScreen
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme

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
                            intent.putExtra("ordenId", orden.id)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}
