package com.thinkthat.mamusckascaner.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componente para mostrar mensajes de error
 */
@Composable
fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = message,
            color = Color(0xFFD32F2F),
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                .padding(12.dp)
        )
        
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("Reintentar", color = Color.White)
            }
        }
    }
}

/**
 * Componente para mostrar mensajes de éxito
 */
@Composable
fun SuccessMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        color = Color(0xFF2E7D32),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFE8F5E8), RoundedCornerShape(8.dp))
            .padding(12.dp)
    )
}

/**
 * Componente para mostrar mensajes informativos
 */
@Composable
fun InfoMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        color = Color(0xFF1976D2),
        fontSize = 14.sp,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
            .padding(12.dp)
    )
}

/**
 * Componente para mostrar mensajes de advertencia
 */
@Composable
fun WarningMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        color = Color(0xFFE65100),
        fontSize = 14.sp,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
            .padding(12.dp)
    )
}

/**
 * Componente para mostrar estado de carga
 */
@Composable
fun LoadingMessage(
    message: String = "Cargando...",
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        CircularProgressIndicator(
            color = Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}
