package com.thinkthat.mamusckascaner.view

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import com.thinkthat.mamusckascaner.domain.model.PedidoRecoleccion
import com.thinkthat.mamusckascaner.presentation.recolectar.RecolectarQrUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecolectarByQRScreen(
    state: RecolectarQrUiState,
    onBack: () -> Unit = {},
    onScanQR: () -> Unit = {},
    onDeletePedido: (Int) -> Unit = {},
    onResumePedido: (PedidoRecoleccion) -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    val pedidosPendientes = state.pedidosPendientes
    val isLoadingPedidos = state.isLoading
    val errorMessage = state.error

    // Estado para diálogos
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var pedidoSeleccionado by remember { mutableStateOf<PedidoRecoleccion?>(null) }

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
    
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
        if (isGranted) {
            onScanQR()
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
                        // El error se limpia al llegar un QR nuevo
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Lista de pedidos pendientes
                if (isLoadingPedidos) {
                    CircularProgressIndicator(color = Color.White)
                } else if (pedidosPendientes.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = screenHeight * 0.3f)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Pedidos Pendientes (${pedidosPendientes.size})",
                                fontSize = bodyFontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCD0914)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            pedidosPendientes.forEach { pedido ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF5F5F5)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Pedido: ${pedido.idPedido}",
                                                fontSize = (bodyFontSize.value * 0.9f).sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = "Depósito: ${pedido.codDeposito}",
                                                fontSize = (bodyFontSize.value * 0.8f).sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "Fecha: ${pedido.fechaCreacion}",
                                                fontSize = (bodyFontSize.value * 0.75f).sp,
                                                color = Color.Gray
                                            )
                                        }
                                        
                                        Row {
                                            // Botón Retomar
                                            IconButton(
                                                onClick = {
                                                    pedidoSeleccionado = pedido
                                                    showResumeDialog = true
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = "Retomar pedido",
                                                    tint = Color(0xFF4CAF50),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(4.dp))
                                            
                                            // Botón Eliminar
                                            IconButton(
                                                onClick = {
                                                    pedidoSeleccionado = pedido
                                                    showDeleteDialog = true
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Eliminar pedido",
                                                    tint = Color(0xFFD32F2F),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
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
        
        // Diálogo de confirmación para eliminar pedido
        if (showDeleteDialog && pedidoSeleccionado != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        text = "Confirmar eliminación",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "¿Está seguro que desea eliminar el pedido ${pedidoSeleccionado!!.idPedido}? " +
                                "Se eliminarán todos los datos guardados para este pedido."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeletePedido(pedidoSeleccionado!!.idPedido)
                            showDeleteDialog = false
                            pedidoSeleccionado = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            pedidoSeleccionado = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Diálogo de confirmación para retomar pedido
        if (showResumeDialog && pedidoSeleccionado != null) {
            AlertDialog(
                onDismissRequest = { showResumeDialog = false },
                title = {
                    Text(
                        text = "Retomar pedido",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "¿Desea retomar el pedido ${pedidoSeleccionado!!.idPedido}? " +
                                "Se consultará la información actualizada del pedido."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val pedido = pedidoSeleccionado!!
                            showResumeDialog = false
                            pedidoSeleccionado = null
                            onResumePedido(pedido)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Retomar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showResumeDialog = false
                            pedidoSeleccionado = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "RecolectarByQR Screen Preview")
@Composable
fun RecolectarByQRScreenPreview() {
    RecolectarByQRScreen(
        state = RecolectarQrUiState(isLoading = false),
        onBack = {},
        onScanQR = {}
    )
}
