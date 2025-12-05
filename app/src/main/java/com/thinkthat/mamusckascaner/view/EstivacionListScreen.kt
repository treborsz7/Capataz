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
import com.thinkthat.mamusckascaner.database.EstivacionEntity
import com.thinkthat.mamusckascaner.utils.AppLogger
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstivacionListScreen(
    onBack: () -> Unit = {},
    onNewEstivacion: () -> Unit = {},
    onResumeEstivacion: (EstivacionEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val coroutineScope = rememberCoroutineScope()
    
    // Database Helper
    val dbHelper = remember { DatabaseHelper(context) }
    
    // Estado para estivaciones pendientes
    var estivacionesPendientes by remember { mutableStateOf<List<EstivacionEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Estado para diálogos
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var estivacionSeleccionada by remember { mutableStateOf<EstivacionEntity?>(null) }
    
    // Responsive values
    val horizontalPadding = maxOf(minOf(screenWidth * 0.08f, 32.dp), 16.dp)
    val titleFontSize = maxOf(minOf((screenWidth * 0.06f).value, 28f), 20f).sp
    val bodyFontSize = maxOf(minOf((screenWidth * 0.04f).value, 18f), 14f).sp
    val buttonHeight = maxOf(minOf(screenHeight * 0.07f, 64.dp), 48.dp)
    
    // Cargar estivaciones pendientes al iniciar
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            estivacionesPendientes = dbHelper.getEstivacionesPendientes()
            AppLogger.logInfo(
                tag = "EstivacionListScreen",
                message = "Cargadas ${estivacionesPendientes.size} estivaciones pendientes"
            )
        } catch (e: Exception) {
            AppLogger.logError(
                tag = "EstivacionListScreen",
                message = "Error al cargar estivaciones pendientes",
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
                text = "Estivación",
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
            } else if (estivacionesPendientes.isEmpty()) {
                // No hay estivaciones pendientes
                Text(
                    text = "No hay estivaciones pendientes",
                    fontSize = bodyFontSize,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                // Lista de estivaciones pendientes
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
                            text = "Estivaciones Pendientes (${estivacionesPendientes.size})",
                            fontSize = bodyFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCD0914)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        estivacionesPendientes.forEach { estivacion ->
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
                                            text = "Partida: ${estivacion.partida}",
                                            fontSize = (bodyFontSize.value * 0.9f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = "Ubicación: ${estivacion.ubicacion}",
                                            fontSize = (bodyFontSize.value * 0.85f).sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "Fecha: ${estivacion.fechaCreacion}",
                                            fontSize = (bodyFontSize.value * 0.75f).sp,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    Row {
                                        // Botón Retomar
                                        IconButton(
                                            onClick = {
                                                estivacionSeleccionada = estivacion
                                                showResumeDialog = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Retomar estivación",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(4.dp))
                                        
                                        // Botón Eliminar
                                        IconButton(
                                            onClick = {
                                                estivacionSeleccionada = estivacion
                                                showDeleteDialog = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Eliminar estivación",
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
            
            // Botón principal para nueva estivación
            Button(
                onClick = onNewEstivacion,
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
                        contentDescription = "Nueva estivación",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Nueva Estivación",
                        color = Color.Black,
                        fontSize = maxOf(minOf((screenWidth * 0.045f).value, 20f), 16f).sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Diálogo de confirmación para eliminar estivación
        if (showDeleteDialog && estivacionSeleccionada != null) {
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
                        text = "¿Está seguro que desea eliminar la estivación de la partida ${estivacionSeleccionada!!.partida}?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val estivacionAEliminar = estivacionSeleccionada!!
                            coroutineScope.launch {
                                try {
                                    dbHelper.deleteEstivacion(estivacionAEliminar.id)
                                    AppLogger.logInfo(
                                        tag = "EstivacionListScreen",
                                        message = "Estivación ${estivacionAEliminar.id} eliminada"
                                    )
                                    // Recargar lista
                                    estivacionesPendientes = dbHelper.getEstivacionesPendientes()
                                } catch (e: Exception) {
                                    AppLogger.logError(
                                        tag = "EstivacionListScreen",
                                        message = "Error al eliminar estivación",
                                        throwable = e
                                    )
                                }
                            }
                            showDeleteDialog = false
                            estivacionSeleccionada = null
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
                            estivacionSeleccionada = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Diálogo de confirmación para retomar estivación
        if (showResumeDialog && estivacionSeleccionada != null) {
            AlertDialog(
                onDismissRequest = { showResumeDialog = false },
                title = {
                    Text(
                        text = "Retomar estivación",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "¿Desea retomar la estivación de la partida ${estivacionSeleccionada!!.partida}?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val estivacion = estivacionSeleccionada!!
                            showResumeDialog = false
                            estivacionSeleccionada = null
                            onResumeEstivacion(estivacion)
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
                            estivacionSeleccionada = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
