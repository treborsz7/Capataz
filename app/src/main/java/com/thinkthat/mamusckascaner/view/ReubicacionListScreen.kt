package com.thinkthat.mamusckascaner.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import com.thinkthat.mamusckascaner.database.DatabaseHelper
import com.thinkthat.mamusckascaner.database.ReubicacionEntity
import com.thinkthat.mamusckascaner.utils.AppLogger
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReubicacionListScreen(
    onBack: () -> Unit = {},
    onNewReubicacion: () -> Unit = {},
    onResumeReubicacion: (ReubicacionEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val coroutineScope = rememberCoroutineScope()
    
    // Database Helper
    val dbHelper = remember { DatabaseHelper(context) }
    
    // Estado para reubicaciones pendientes
    var reubicacionesPendientes by remember { mutableStateOf<List<ReubicacionEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Estado para diálogos
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var reubicacionSeleccionada by remember { mutableStateOf<ReubicacionEntity?>(null) }
    
    // Responsive values
    val horizontalPadding = maxOf(minOf(screenWidth * 0.08f, 32.dp), 16.dp)
    val titleFontSize = maxOf(minOf((screenWidth * 0.06f).value, 28f), 20f).sp
    val bodyFontSize = maxOf(minOf((screenWidth * 0.04f).value, 18f), 14f).sp
    val buttonHeight = maxOf(minOf(screenHeight * 0.07f, 64.dp), 48.dp)
    
    // Cargar reubicaciones pendientes al iniciar
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            reubicacionesPendientes = dbHelper.getReubicacionesPendientes()
            AppLogger.logInfo(
                tag = "ReubicacionListScreen",
                message = "Cargadas ${reubicacionesPendientes.size} reubicaciones pendientes"
            )
        } catch (e: Exception) {
            AppLogger.logError(
                tag = "ReubicacionListScreen",
                message = "Error al cargar reubicaciones pendientes",
                throwable = e
            )
        } finally {
            isLoading = false
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
                text = "Reubicar",
                fontSize = titleFontSize,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Contenido principal
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else if (reubicacionesPendientes.isEmpty()) {
                // No hay reubicaciones pendientes
                Text(
                    text = "No hay reubicaciones pendientes",
                    fontSize = bodyFontSize,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                // Lista de reubicaciones pendientes
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = screenHeight * 0.5f)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Reubicaciones Pendientes (${reubicacionesPendientes.size})",
                            fontSize = bodyFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCD0914)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        reubicacionesPendientes.forEach { reubicacion ->
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
                                            text = "Partida: ${reubicacion.partida}",
                                            fontSize = (bodyFontSize.value * 0.9f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = "Origen: ${reubicacion.ubicacionOrigen}",
                                            fontSize = (bodyFontSize.value * 0.85f).sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "Destino: ${reubicacion.ubicacionDestino}",
                                            fontSize = (bodyFontSize.value * 0.85f).sp,
                                            color = Color(0xFF2196F3)
                                        )
                                        Text(
                                            text = "Fecha: ${reubicacion.fechaCreacion}",
                                            fontSize = (bodyFontSize.value * 0.75f).sp,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    Row {
                                        // Botón Retomar
                                        IconButton(
                                            onClick = {
                                                reubicacionSeleccionada = reubicacion
                                                showResumeDialog = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Retomar reubicación",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(4.dp))
                                        
                                        // Botón Eliminar
                                        IconButton(
                                            onClick = {
                                                reubicacionSeleccionada = reubicacion
                                                showDeleteDialog = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Eliminar reubicación",
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
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Botón principal para nueva reubicación
            Button(
                onClick = onNewReubicacion,
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
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Nueva reubicación",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Nueva Reubicación",
                        color = Color.Black,
                        fontSize = maxOf(minOf((screenWidth * 0.045f).value, 20f), 16f).sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Diálogo de confirmación para eliminar reubicación
        if (showDeleteDialog && reubicacionSeleccionada != null) {
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
                        text = "¿Está seguro que desea eliminar la reubicación de la partida ${reubicacionSeleccionada!!.partida}?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val reubicacionAEliminar = reubicacionSeleccionada!!
                            coroutineScope.launch {
                                try {
                                    dbHelper.deleteReubicacion(reubicacionAEliminar.id)
                                    AppLogger.logInfo(
                                        tag = "ReubicacionListScreen",
                                        message = "Reubicación ${reubicacionAEliminar.id} eliminada"
                                    )
                                    // Recargar lista
                                    reubicacionesPendientes = dbHelper.getReubicacionesPendientes()
                                } catch (e: Exception) {
                                    AppLogger.logError(
                                        tag = "ReubicacionListScreen",
                                        message = "Error al eliminar reubicación",
                                        throwable = e
                                    )
                                }
                            }
                            showDeleteDialog = false
                            reubicacionSeleccionada = null
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
                            reubicacionSeleccionada = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Diálogo de confirmación para retomar reubicación
        if (showResumeDialog && reubicacionSeleccionada != null) {
            AlertDialog(
                onDismissRequest = { showResumeDialog = false },
                title = {
                    Text(
                        text = "Retomar reubicación",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "¿Desea retomar la reubicación de la partida ${reubicacionSeleccionada!!.partida}?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val reubicacion = reubicacionSeleccionada!!
                            showResumeDialog = false
                            reubicacionSeleccionada = null
                            onResumeReubicacion(reubicacion)
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
                            reubicacionSeleccionada = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
