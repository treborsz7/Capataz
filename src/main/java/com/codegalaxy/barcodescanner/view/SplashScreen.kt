package com.codegalaxy.barcodescanner.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.lifecycleScope
import com.codegalaxy.barcodescanner.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                //Image(painter = painterResource(id = R.drawable.splash_image), contentDescription = "Splash Image")
            }
        }

        lifecycleScope.launch {
            delay(3000) // 3 seconds delay
            android.util.Log.d("LoginActivity", "LoginActivity is being created")

            val intent = Intent(this@SplashScreen, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Ensure SplashScreen finishes after starting LoginActivity
        }
    }
}
