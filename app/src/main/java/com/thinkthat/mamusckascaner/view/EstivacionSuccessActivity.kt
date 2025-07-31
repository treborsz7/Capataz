package com.thinkthat.mamusckascaner.view

import EstivacionSuccessScreen
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme


class EstivacionSuccessActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarCodeScannerTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    EstivacionSuccessScreen(
                        onFinish = {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

