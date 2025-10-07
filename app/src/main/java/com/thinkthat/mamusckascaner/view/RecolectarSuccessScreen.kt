package com.thinkthat.mamusckascaner.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RecolectarSuccessScreen(
    onFinish: () -> Unit
) {
    var showSuccess by remember { mutableStateOf(false) }

    // Pantalla de carga por 2 segundos
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showSuccess = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCD0914)),
        contentAlignment = Alignment.Center
    ) {
        if (!showSuccess) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Guardando Recolección...", color = Color.White)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¡Recolección guardada correctamente!",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Finalizar", color = Color.Black)
                }
            }
        }
    }
}
