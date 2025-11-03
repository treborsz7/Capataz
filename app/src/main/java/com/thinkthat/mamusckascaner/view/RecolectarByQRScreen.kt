package com.thinkthat.mamusckascaner.view

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalConfiguration
import com.thinkthat.mamusckascaner.utils.AppLogger
import com.thinkthat.mamusckascaner.utils.QRData
import com.thinkthat.mamusckascaner.utils.parseQRData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecolectarByQRScreen(
    onBack: () -> Unit = {},
    onScanQR: () -> Unit = {},
    scannedResult: String? = null,
    onProceedWithOrder: (String, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // Responsive values
    val horizontalPadding = maxOf(minOf(screenWidth * 0.08f, 32.dp), 16.dp)
    val iconSize = maxOf(minOf(screenWidth * 0.3f, 140.dp), 80.dp)
    val titleFontSize = maxOf(minOf((screenWidth * 0.06f).value, 28f), 20f).sp
    val bodyFontSize = maxOf(minOf((screenWidth * 0.04f).value, 18f), 14f).sp
    val buttonHeight = maxOf(minOf(screenHeight * 0.07f, 64.dp), 48.dp)
    val spacing = maxOf(minOf(screenHeight * 0.04f, 32.dp), 16.dp)
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var optimizaRecorrido by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
        if (isGranted) {
            onScanQR()
        }
    }

    // Auto-redirigir cuando se escanea un QR válido
    LaunchedEffect(scannedResult) {
        if (scannedResult != null) {
            val parsedData = parseQRData(scannedResult)
            
            // Validar que el QR tenga los datos esperados
            if (parsedData.deposito.isNotEmpty() && parsedData.pedido.isNotEmpty()) {
                // QR válido - limpiar error y redirigir automáticamente
                errorMessage = null
                onProceedWithOrder(scannedResult, optimizaRecorrido)
            } else {
                // QR inválido - mostrar error
                AppLogger.logError(
                    tag = "RecolectarByQRScreen",
                    message = "QR inválido sin datos esperados: $scannedResult"
                )
                errorMessage = "No se pudo procesar el QR escaneado."
            }
        } else {
            // Limpiar error cuando se resetea el resultado escaneado
            errorMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCD0914))
            .padding(WindowInsets.systemBars.asPaddingValues()),
        contentAlignment = Alignment.Center
    ) {
        // Botón de volver en la esquina superior izquierda
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontalPadding / 2)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
            }
        }
        
        // Título en la parte superior
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = horizontalPadding / 2)
        ) {
            Text(
                text = "Recolectar",
                fontSize = titleFontSize,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Contenido principal centrado
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = horizontalPadding)
        ) {
            // Ícono principal
            Icon(
                imageVector = Icons.Filled.QrCodeScanner,
                contentDescription = "Escanear QR",
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
            
            Spacer(modifier = Modifier.height(spacing))
            
            if (errorMessage != null) {
                // Mostrar mensaje de error
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding / 2),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontalPadding / 2),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error:",
                            fontSize = bodyFontSize,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            fontSize = bodyFontSize,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(spacing))
                
                // Botón para intentar escanear de nuevo
                Button(
                    onClick = {
                        errorMessage = null
                        if (hasCameraPermission) {
                            onScanQR()
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Intentar de nuevo",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Intentar de nuevo",
                            color = Color.Black,
                            fontSize = maxOf(minOf((screenWidth * 0.045f).value, 20f), 16f).sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            
            } else {
                // Estado inicial - sin resultado
                Text(
                    text = "Escanea el código QR del pedido que deseas recolectar",
                    fontSize = bodyFontSize,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Checkbox para optimizar recorrido
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "¿Optimizar Recorrido?",
                            fontSize = bodyFontSize,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Checkbox(
                            checked = optimizaRecorrido,
                            onCheckedChange = { optimizaRecorrido = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.White,
                                uncheckedColor = Color.White,
                                checkmarkColor = Color(0xFFCD0914)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Botón principal de escanear
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onScanQR()
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Escanear QR",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Escanear QR",
                            color = Color.Black,
                            fontSize = maxOf(minOf((screenWidth * 0.045f).value, 20f), 16f).sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "RecolectarByQR Screen Preview")
@Composable
fun RecolectarByQRScreenPreview() {
    RecolectarByQRScreen(
        onBack = {},
        onScanQR = {}
    )
}
