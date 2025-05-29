package com.codegalaxy.barcodescanner.view

import BarcodeScannerScreen
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.codegalaxy.barcodescanner.ui.theme.BarCodeScannerTheme
import com.codegalaxy.barcodescanner.viewmodel.BarCodeScannerViewModel
import androidx.compose.foundation.layout.fillMaxSize

class BarcodeScannerActivity : ComponentActivity() {

    private val viewModel: BarCodeScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarCodeScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    BarcodeScannerScreen(
                        viewModel = viewModel,
                        onBack = { finish() },
                        onScanResult = { result ->
                            val intent = Intent().apply {
                                putExtra("scanResult", result)
                            }
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        },
                        scannerKey = 0
                    )
                }
            }
        }
    }
}

