package com.thinkthat.mamusckascaner.view
import android.content.res.Resources.Theme
import android.os.Bundle
import android.window.SplashScreen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarCodeScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    /*SplashScreen(


                    )*/
                }
            }
        }
    }
}