package com.thinkthat.mamusckascaner.view

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinkthat.mamusckascaner.view.components.ErrorMessage
import com.thinkthat.mamusckascaner.view.components.LoadingMessage

import com.thinkthat.mamusckascaner.domain.model.UbicacionRecolectar
import com.thinkthat.mamusckascaner.presentation.recolectar.RecolectarUiState
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecolectarScreen(
    state: RecolectarUiState,
    onBack: () -> Unit = {},
    onClose: () -> Unit = {},
    /** (codArticulo, índice de escaneo, campo: "partida" | "ubicacion") */
    onEscanear: (String, Int, String) -> Unit = { _, _, _ -> },
    onPartidaChange: (String, Int, String) -> Unit = { _, _, _ -> },
    onUbicacionChange: (String, Int, String) -> Unit = { _, _, _ -> },
    onCantidadChange: (String, Int, String) -> Unit = { _, _, _ -> },
    onAsegurarEscaneos: (String, Int) -> Unit = { _, _ -> },
    /** (codArticulo, índice, marcar la cantidad como confirmada) */
    onGuardarRenglon: (String, Int, Boolean) -> Unit = { _, _, _ -> },
    /** Vuelve a habilitar la edición de una cantidad ya confirmada. */
    onEditarCantidad: (String, Int) -> Unit = { _, _ -> },
    onEliminarRenglon: (String, Int) -> Unit = { _, _ -> },
    onRetryUbicaciones: () -> Unit = {},
    onEnviar: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onSuccess: () -> Unit = {}
) {
    // Todo lo que se dibuja sale del estado; la pantalla no consulta API ni base.
    val ubicaciones = state.ubicaciones
    val isLoadingUbicaciones = state.isLoadingUbicaciones
    val errorUbicaciones = state.errorUbicaciones
    val scaneoIndividual = state.escaneos
    val cantidadesPorArticulo = state.cantidades
    val cantidadesGuardadas = state.cantidadesGuardadas
    val errorEnvio = state.errorEnvio
    val isLoadingEnvio = state.isEnviando
    val showSuccessScreen = state.envioExitoso
    val idPedido = state.idPedido

    // Estado de interacción, propio de la pantalla
    var showBackDialog by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }
    var showDeleteRenglonDialog by remember { mutableStateOf(false) }
    var renglonAEliminar by remember { mutableStateOf<Pair<String, Int>?>(null) } // codArticulo, indice

    // Qué campos están en modo edición: codArticulo -> índice -> campo -> editable
    var camposEditables by remember { mutableStateOf(mapOf<String, Map<Int, Map<String, Boolean>>>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCD0914))

            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        // Estructura principal: columna con header fijo arriba y contenido scrolleable abajo
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header fijo (no scrolleable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showBackDialog = true }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Recolectar",
                        fontSize = 24.sp,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showCloseDialog = true }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
            }
            
            // Contenido scrolleable
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier

                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    
            ) {

            // Mostrar estado de carga de la orden
            /*if (isLoadingOrden) {
                LoadingMessage(
                    message = "Cargando orden...",
                    modifier = Modifier.padding(16.dp)
                )
            }
*/



            // Mostrar error de ubicaciones si existe
            if (errorUbicaciones != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ErrorMessage(
                        message = errorUbicaciones!!,
                        modifier = Modifier.fillMaxWidth(),
                        onDismiss = { /* errorUbicaciones se maneja en RecolectarActivity */ }
                    )
                    
                    // Botón de reintentar
                    Button(
                        onClick = onRetryUbicaciones,
                        modifier = Modifier.fillMaxWidth(0.6f),
                        colors = ButtonDefaults.buttonColors(

                            containerColor = Color(0xFFCD0914)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Reintentar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reintentar", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Información del pedido (sin mostrar depósito)
            if (idPedido > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Información del QR:",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Pedido: $idPedido",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                }
            }
            // Campo depósito oculto en todas las pantallas

            // Mostrar estado de carga de ubicaciones
            if (isLoadingUbicaciones) {
                LoadingMessage(
                    message = "Cargando ubicaciones para recolectar...",
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Mostrar información de las ubicaciones para recolectar
            if (ubicaciones.isNotEmpty()) {
                // Una tarjeta por artículo, con sus ubicaciones en el orden que las devolvió la API
                val ubicacionesGrouped = ubicaciones.groupBy { it.codArticulo }

                // Estado para dropdowns expandidos
                var expandedItems by remember { mutableStateOf(setOf<String>()) }

                            
                            ubicacionesGrouped.forEach { (codArticulo, ubicacionesDelArticulo) ->
                                // Se activan tantas ubicaciones como haga falta para cubrir
                                // la cantidad requerida, sumando el saldo disponible de cada una.
                                LaunchedEffect(codArticulo, ubicacionesDelArticulo.size) {
                                    val cantidadRequerida = ubicacionesDelArticulo.firstOrNull()?.requerido ?: 0.0

                                    var escaneosPorActivar = 0
                                    var cantidadAcumulada = 0.0
                                    for (i in ubicacionesDelArticulo.indices) {
                                        escaneosPorActivar = i + 1
                                        cantidadAcumulada += ubicacionesDelArticulo[i].saldoDisponible
                                        if (cantidadAcumulada >= cantidadRequerida) break
                                    }
                                    escaneosPorActivar = escaneosPorActivar
                                        .coerceAtLeast(1)
                                        .coerceAtMost(ubicacionesDelArticulo.size)

                                    onAsegurarEscaneos(codArticulo, escaneosPorActivar)
                                }
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF8F8F8)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        // Título del artículo - obtener descripción del primer elemento del grupo
                                        val descripcionArticulo = ubicacionesDelArticulo.firstOrNull()?.descripcionArticulo ?: "N/A"
                                        Text(
                                            text = descripcionArticulo,
                                            color = Color.Black,
                                            fontSize = 16.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Código Artículo: $codArticulo",
                                            color = Color.Black,
                                            fontSize = 16.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Mostrar cantidad solicitada y recolectada
                                        val cantidadSolicitada = ubicacionesDelArticulo.firstOrNull()?.requerido ?: 0.0
                                        
                                        // Calcular total recolectado de todos los escaneos
                                        val listaEscaneos = scaneoIndividual[codArticulo] ?: emptyList()
                                        val cantidadesArticulo = cantidadesPorArticulo[codArticulo] ?: emptyMap()
                                        val cantidadesGuardadasArticulo = cantidadesGuardadas[codArticulo] ?: emptyMap()
                                        
                                        val totalRecolectado = listaEscaneos.indices.sumOf { indice ->
                                            val guardado = cantidadesGuardadasArticulo[indice] ?: false
                                            if (guardado) {
                                                cantidadesArticulo[indice]?.toDoubleOrNull() ?: 0.0
                                            } else {
                                                0.0
                                            }
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Solicitado: ${if (cantidadSolicitada % 1.0 == 0.0) cantidadSolicitada.toInt().toString() else cantidadSolicitada.toString()}",
                                                color = Color.Black,
                                                fontSize = 14.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                                            )
                                            Text(
                                                text = "Recolectado: ${if (totalRecolectado % 1.0 == 0.0) totalRecolectado.toInt().toString() else totalRecolectado.toString()}",
                                                color = if (totalRecolectado >= cantidadSolicitada) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                                fontSize = 14.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Renderizar cada escaneo guardado (con cantidad confirmada)
                                        listaEscaneos.forEachIndexed { indice, escaneo ->
                                            val partidaEscaneado: String? = escaneo.partida.takeIf { it.isNotEmpty() }
                                            val ubicacionEscaneada: String? = escaneo.ubicacion.takeIf { it.isNotEmpty() }
                                            val cantidadEscaneo = cantidadesArticulo[indice] ?: ""
                                            val cantidadGuardada = cantidadesGuardadasArticulo[indice] ?: false
                                            
                                            val camposArticulo = camposEditables[codArticulo] ?: emptyMap()
                                            val camposIndice = camposArticulo[indice] ?: mapOf("partida" to false, "ubicacion" to false)
                                            val partidaEditable = camposIndice["partida"] ?: false
                                            val ubicacionEditable = camposIndice["ubicacion"] ?: false
                                            
                                            // Auto-llenar cantidad cuando se completen partida y ubicación
                                            LaunchedEffect(partidaEscaneado, ubicacionEscaneada, cantidadSolicitada, totalRecolectado) {
                                                if (partidaEscaneado?.isNotEmpty() == true && 
                                                    ubicacionEscaneada?.isNotEmpty() == true &&
                                                    cantidadEscaneo.isEmpty() &&
                                                    cantidadSolicitada > 0) {
                                                    val restante = cantidadSolicitada - totalRecolectado
                                                    val cantidadAutoLlenar = if (restante > 0) restante else cantidadSolicitada
                                                    val cantidadStr = if (cantidadAutoLlenar % 1.0 == 0.0) cantidadAutoLlenar.toInt().toString() else cantidadAutoLlenar.toString()
                                                    onCantidadChange(codArticulo, indice, cantidadStr)
                                                }
                                            }
                                            
                                            // Validar datos del escaneo actual
                                            val ubicacionAsignada = ubicacionesDelArticulo.getOrNull(indice)
                                            val nombreUbicacionEsperado = ubicacionAsignada?.nombreUbicacion
                                            val nroPartidaEsperado = ubicacionAsignada?.nroPartida
                                            
                                            // Validación de ubicación
                                            val ubicacionValida = ubicacionEscaneada == nombreUbicacionEsperado
                                            
                                            // Validación de partida (solo si existe un nroPartida esperado válido)
                                            val partidaValida = if (nroPartidaEsperado != null && nroPartidaEsperado != "N/A") {
                                                partidaEscaneado == nroPartidaEsperado
                                            } else {
                                                true // Si no hay partida esperada, es válido
                                            }
                                            
                                            // Determinar si el escaneo completo es válido
                                            val escaneoValido = ubicacionValida && partidaValida
                                            
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (!escaneoValido && !ubicacionEscaneada.isNullOrEmpty()) {
                                                        Color(0xFFFFEBEE) // Rojo claro si hay datos incorrectos
                                                    } else if (cantidadGuardada) {
                                                        Color(0xFFE8F5E9) // Verde claro si está guardado
                                                    } else {
                                                        Color(0xFFFFF3E0) // Naranja claro por defecto
                                                    }
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp)
                                                ) {
                                                    // Mostrar número de escaneo y ubicación asignada
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                                        // Mostrar ubicación asignada y nroPartida
                                                        if (indice < ubicacionesDelArticulo.size) {
                                                            val ubicacionAsignada = ubicacionesDelArticulo[indice]
                                                            
                                                            // Log para debuggear
                                                            Log.d("RecolectarScreen", "Ubicación asignada: $ubicacionAsignada")
                                                            Log.d("RecolectarScreen", "nroPartida: ${ubicacionAsignada.nroPartida}")
                                                            
                                                            val nroPartida = ubicacionAsignada.nroPartida
                                                            val saldoDisponible = ubicacionAsignada.saldoDisponible
                                                            
                                                            Column {
                                                                Text(
                                                                    text = ubicacionAsignada.nombreUbicacion,
                                                                    fontSize = 14.sp,
                                                                    color = Color(0xFF4CAF50),
                                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                                )
                                                                Text(
                                                                    text = "Partida: $nroPartida",
                                                                    fontSize = 12.sp,
                                                                    color = Color(0xFF757575),
                                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                                                                )
                                                                Text(
                                                                    text = "Disponible: ${if (saldoDisponible % 1.0 == 0.0) saldoDisponible.toInt().toString() else saldoDisponible.toString()}",
                                                                    fontSize = 12.sp,
                                                                    color = Color(0xFF2196F3),
                                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                                                                )
                                                            }
                                                        } else {
                                                            Text(
                                                                text = "Ubicación ${indice + 1}",
                                                                fontSize = 14.sp,
                                                                color = Color.Gray,
                                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                                            )
                                                        }
                                                        
                                                        // Botón X para eliminar el renglón (solo si hay más de un renglón)
                                                        if (listaEscaneos.size > 1) {
                                                            IconButton(
                                                                onClick = {
                                                                    // Mostrar diálogo de confirmación
                                                                    renglonAEliminar = Pair(codArticulo, indice)
                                                                    showDeleteRenglonDialog = true
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    Icons.Filled.Close,
                                                                    contentDescription = "Eliminar renglón",
                                                                    tint = Color.Red,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    // Campo Partida
                                                    if (partidaEscaneado?.isEmpty() != false) {
                                                        // Partida vacía - mostrar campo y botón escanear
                                                        if (!partidaEditable) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .background(Color.Transparent)
                                                                        .padding(16.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "Partida: \n ",
                                                                        color = Color.Black,
                                                                        fontSize = 16.sp
                                                                    )
                                                                }
                                                                IconButton(
                                                                    onClick = {
                                                                        val camposArt = camposEditables[codArticulo] ?: emptyMap()
                                                                        val camposInd = camposArt[indice] ?: mapOf("partida" to false, "ubicacion" to false)
                                                                        camposEditables = camposEditables + (codArticulo to (camposArt + (indice to (camposInd + ("partida" to true)))))
                                                                    }
                                                                ) {
                                                                    Icon(
                                                                        Icons.Filled.Edit,
                                                                        contentDescription = "Editar partida",
                                                                        tint = Color.Black,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            // Obtener nroPartida esperado de esta ubicación
                                                            val ubicacionAsignada = ubicacionesDelArticulo.getOrNull(indice)
                                                            val nroPartidaEsperado = ubicacionAsignada?.nroPartida
                                                            
                                                            // Validar partida: solo si nroPartidaEsperado no es null ni "N/A"
                                                            val partidaValida = if (nroPartidaEsperado != null && nroPartidaEsperado != "N/A") {
                                                                partidaEscaneado == nroPartidaEsperado
                                                            } else {
                                                                true // Si no hay partida esperada, siempre es válido
                                                            }
                                                            
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                OutlinedTextField(
                                                                    value = partidaEscaneado ?: "",
                                                                    onValueChange = { newValue ->
                                                                        onPartidaChange(codArticulo, indice, newValue)
                                                                    },
                                                                    label = { Text("Partida", color = Color.Black) },
                                                                    singleLine = true,
                                                                    modifier = Modifier.weight(1f),
                                                                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                                    colors = OutlinedTextFieldDefaults.colors(
                                                                        cursorColor = Color.Black,
                                                                        focusedBorderColor = if (partidaEscaneado.isNullOrEmpty()) Color.Black 
                                                                                             else if (partidaValida) Color(0xFF4CAF50) 
                                                                                             else Color(0xFFFF5252),
                                                                        unfocusedBorderColor = if (partidaEscaneado.isNullOrEmpty()) Color.Black 
                                                                                               else if (partidaValida) Color(0xFF4CAF50) 
                                                                                               else Color(0xFFFF5252),
                                                                        focusedLabelColor = Color.Black,
                                                                        unfocusedLabelColor = Color.Black
                                                                    ),
                                                                    placeholder = { Text("Código del partida", color = Color.Gray) }
                                                                )
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                IconButton(
                                                                    onClick = {
                                                                        val camposArt = camposEditables[codArticulo] ?: emptyMap()
                                                                        val camposInd = camposArt[indice] ?: mapOf("partida" to false, "ubicacion" to false)
                                                                        camposEditables = camposEditables + (codArticulo to (camposArt + (indice to (camposInd + ("partida" to false)))))
                                                                    }
                                                                ) {
                                                                    Icon(
                                                                        Icons.Filled.Save,
                                                                        contentDescription = "Guardar partida",
                                                                        tint = if (partidaEscaneado != "") Color(0xFF4CAF50) else Color.Gray,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        
                                                        Button(
                                                            onClick = {
                                                                onEscanear(codArticulo, indice, "partida")
                                                            },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color.Red
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.QrCodeScanner,
                                                                    contentDescription = "Escanear partida",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = "Escanear Partida",
                                                                    color = Color.White,
                                                                    fontSize = 12.sp
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        // Partida ya escaneada
                                                        // Validar partida
                                                        val ubicacionAsignadaValidacion = ubicacionesDelArticulo.getOrNull(indice)
                                                        val nroPartidaEsperado = ubicacionAsignadaValidacion?.nroPartida
                                                        val partidaEsCorrecta = if (nroPartidaEsperado != null && nroPartidaEsperado != "N/A") {
                                                            partidaEscaneado == nroPartidaEsperado
                                                        } else {
                                                            true // Si no hay partida esperada, siempre es correcta
                                                        }
                                                        
                                                        if (!partidaEditable) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .background(Color.Transparent)
                                                                        .padding(16.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "Partida:\n$partidaEscaneado",
                                                                        color = if (partidaEsCorrecta) Color.Black else Color(0xFFFF5252),
                                                                        fontSize = 16.sp,
                                                                        fontWeight = if (!partidaEsCorrecta) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                                                    )
                                                                }
                                                                IconButton(
                                                                    onClick = {
                                                                        onEscanear(codArticulo, indice, "partida")
                                                                    }
                                                                ) {
                                                                    Icon(
                                                                        Icons.Filled.CameraAlt,
                                                                        contentDescription = "Escanear partida",
                                                                        tint = Color.Black,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                }
                                                                IconButton(
                                                                    onClick = {
                                                                        val camposArt = camposEditables[codArticulo] ?: emptyMap()
                                                                        val camposInd = camposArt[indice] ?: mapOf("partida" to false, "ubicacion" to false)
                                                                        camposEditables = camposEditables + (codArticulo to (camposArt + (indice to (camposInd + ("partida" to true)))))
                                                                    }
                                                                ) {
                                                                    Icon(
                                                                        Icons.Filled.Edit,
                                                                        contentDescription = "Editar partida",
                                                                        tint = Color.Black,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            // Obtener nroPartida esperado de esta ubicación
                                                            val ubicacionAsignada = ubicacionesDelArticulo.getOrNull(indice)
                                                            val nroPartidaEsperado = ubicacionAsignada?.nroPartida
                                                            
                                                            // Validar partida: solo si nroPartidaEsperado no es null ni "N/A"
                                                            val partidaValida = if (nroPartidaEsperado != null && nroPartidaEsperado != "N/A") {
                                                                partidaEscaneado == nroPartidaEsperado
                                                            } else {
                                                                true // Si no hay partida esperada, siempre es válido
                                                            }
                                                            
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                OutlinedTextField(
                                                                    value = partidaEscaneado,
                                                                    onValueChange = { newValue ->
                                                                        onPartidaChange(codArticulo, indice, newValue)
                                                                    },
                                                                    label = { Text("Partida", color = Color.Black) },
                                                                    singleLine = true,
                                                                    modifier = Modifier.weight(1f),
                                                                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                                    colors = OutlinedTextFieldDefaults.colors(
                                                                        cursorColor = Color.Black,
                                                                        focusedBorderColor = if (partidaValida) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                                                        unfocusedBorderColor = if (partidaValida) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                                                        focusedLabelColor = Color.Black,
                                                                        unfocusedLabelColor = Color.Black
                                                                    ),
                                                                    placeholder = { Text("Numero del Partida", color = Color.Gray) }
                                                                )
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                IconButton(
                                                                    onClick = {
                                                                        val camposArt = camposEditables[codArticulo] ?: emptyMap()
                                                                        val camposInd = camposArt[indice] ?: mapOf("partida" to false, "ubicacion" to false)
                                                                        camposEditables = camposEditables + (codArticulo to (camposArt + (indice to (camposInd + ("partida" to false)))))
                                                                        
                                                                        // La persistencia la resuelve el ViewModel
                                                                        onGuardarRenglon(codArticulo, indice, false)
                                                                    }
                                                                ) {
                                                                    Icon(
                                                                        Icons.Filled.Save,
                                                                        contentDescription = "Guardar Partida",
                                                                        tint = if (partidaEscaneado.isNotEmpty()) Color(0xFF4CAF50) else Color.Gray,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        
                                                        // Campo Ubicación (solo si partida está completa)
                                                        if (ubicacionEscaneada?.isEmpty() != false) {
                                                            if (!ubicacionEditable) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .weight(1f)
                                                                            .background(Color.Transparent)
                                                                            .padding(16.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = "Ubicación: \n",
                                                                            color = Color.Black,
                                                                            fontSize = 16.sp
                                                                        )
                                                                    }
                                                                    IconButton(
                                                                        onClick = {
                                                                            val camposArt = camposEditables[codArticulo] ?: emptyMap()
                                                                            val camposInd = camposArt[indice] ?: mapOf("partida" to false, "ubicacion" to false)
                                                                            camposEditables = camposEditables + (codArticulo to (camposArt + (indice to (camposInd + ("ubicacion" to true)))))
                                                                        }
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Filled.Edit,
                                                                            contentDescription = "Editar ubicación",
                                                                            tint = Color.Black,
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                }
                                                            } else {
                                                                // Obtener ubicación esperada
                                                                val ubicacionAsignada = ubicacionesDelArticulo.getOrNull(indice)
                                                                val nombreUbicacionEsperado = ubicacionAsignada?.nombreUbicacion
                                                                
                                                                // Validar ubicación
                                                                val ubicacionValida = ubicacionEscaneada == nombreUbicacionEsperado
                                                                
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    OutlinedTextField(
                                                                        value = ubicacionEscaneada ?: "",
                                                                        onValueChange = { newValue ->
                                                                            onUbicacionChange(codArticulo, indice, newValue)
                                                                        },
                                                                        label = { Text("Ubicación", color = Color.Black) },
                                                                        singleLine = true,
                                                                        modifier = Modifier.weight(1f),
                                                                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                                        colors = OutlinedTextFieldDefaults.colors(
                                                                            cursorColor = Color.Black,
                                                                            focusedBorderColor = if (ubicacionEscaneada.isNullOrEmpty()) Color.Black 
                                                                                                 else if (ubicacionValida) Color(0xFF4CAF50) 
                                                                                                 else Color(0xFFFF5252),
                                                                            unfocusedBorderColor = if (ubicacionEscaneada.isNullOrEmpty()) Color.Black 
                                                                                                   else if (ubicacionValida) Color(0xFF4CAF50) 
                                                                                                   else Color(0xFFFF5252),
                                                                            focusedLabelColor = Color.Black,
                                                                            unfocusedLabelColor = Color.Black
                                                                        ),
                                                                        placeholder = { Text("Código de ubicación", color = Color.Gray) }
                                                                    )
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    IconButton(
                                                                        onClick = {
                                                                            val camposArt = camposEditables[codArticulo] ?: emptyMap()
                                                                            val camposInd = camposArt[indice] ?: mapOf("partida" to false, "ubicacion" to false)
                                                                            camposEditables = camposEditables + (codArticulo to (camposArt + (indice to (camposInd + ("ubicacion" to false)))))
                                                                        }
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Filled.Save,
                                                                            contentDescription = "Guardar ubicación",
                                                                            tint = if (ubicacionEscaneada != "") Color(0xFF4CAF50) else Color.Gray,
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            
                                                            Button(
                                                                onClick = {
                                                                    onEscanear(codArticulo, indice, "ubicacion")
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = Color.Red
                                                                ),
                                                                shape = RoundedCornerShape(8.dp)
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Filled.LocationOn,
                                                                        contentDescription = "Escanear ubicación",
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size(24.dp)
                                                                    )
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text(
                                                                        text = "Escanear Ubicación",
                                                                        color = Color.White,
                                                                        fontSize = 12.sp
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            // Ubicación ya escaneada
                                                            // Validar ubicación
                                                            val ubicacionAsignadaValidacion = ubicacionesDelArticulo.getOrNull(indice)
                                                            val nombreUbicacionEsperado = ubicacionAsignadaValidacion?.nombreUbicacion
                                                            val ubicacionEsCorrecta = ubicacionEscaneada == nombreUbicacionEsperado
                                                            
                                                            if (!ubicacionEditable) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .weight(1f)
                                                                            .background(Color.Transparent)
                                                                            .padding(16.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = "Ubicación:\n$ubicacionEscaneada",
                                                                            color = if (ubicacionEsCorrecta) Color.Black else Color(0xFFFF5252),
                                                                            fontSize = 16.sp,
                                                                            fontWeight = if (!ubicacionEsCorrecta) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                                                        )
                                                                    }
                                                                    IconButton(
                                                                        onClick = {
                                                                            onEscanear(codArticulo, indice, "ubicacion")
                                                                        }
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Filled.CameraAlt,
                                                                            contentDescription = "Escanear ubicación",
                                                                            tint = Color.Black,
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                    IconButton(
                                                                        onClick = {
                                                                            val camposArt = camposEditables[codArticulo] ?: emptyMap()
                                                                            val camposInd = camposArt[indice] ?: mapOf("partida" to false, "ubicacion" to false)
                                                                            camposEditables = camposEditables + (codArticulo to (camposArt + (indice to (camposInd + ("ubicacion" to true)))))
                                                                        }
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Filled.Edit,
                                                                            contentDescription = "Editar ubicación",
                                                                            tint = Color.Black,
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                }
                                                            } else {
                                                                // Obtener ubicación esperada
                                                                val ubicacionAsignada = ubicacionesDelArticulo.getOrNull(indice)
                                                                val nombreUbicacionEsperado = ubicacionAsignada?.nombreUbicacion
                                                                
                                                                // Validar ubicación
                                                                val ubicacionValida = ubicacionEscaneada == nombreUbicacionEsperado
                                                                
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    OutlinedTextField(
                                                                        value = ubicacionEscaneada,
                                                                        onValueChange = { newValue ->
                                                                            onUbicacionChange(codArticulo, indice, newValue)
                                                                        },
                                                                        label = { Text("Ubicación", color = Color.Black) },
                                                                        singleLine = true,
                                                                        modifier = Modifier.weight(1f),
                                                                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                                        colors = OutlinedTextFieldDefaults.colors(
                                                                            cursorColor = Color.Black,
                                                                            focusedBorderColor = if (ubicacionValida) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                                                            unfocusedBorderColor = if (ubicacionValida) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                                                            focusedLabelColor = Color.Black,
                                                                            unfocusedLabelColor = Color.Black
                                                                        ),
                                                                        placeholder = { Text("Código de ubicación", color = Color.Gray) }
                                                                    )
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    IconButton(
                                                                        onClick = {
                                                                            val camposArt = camposEditables[codArticulo] ?: emptyMap()
                                                                            val camposInd = camposArt[indice] ?: mapOf("partida" to false, "ubicacion" to false)
                                                                            camposEditables = camposEditables + (codArticulo to (camposArt + (indice to (camposInd + ("ubicacion" to false)))))
                                                                            
                                                                            // La persistencia la resuelve el ViewModel
                                                                            onGuardarRenglon(codArticulo, indice, false)
                                                                        }
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Filled.Save,
                                                                            contentDescription = "Guardar ubicación",
                                                                            tint = if (ubicacionEscaneada.isNotEmpty()) Color(0xFF4CAF50) else Color.Gray,
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            
                                                            // Campo cantidad (solo si partida y ubicación están completos)
                                                            if (!cantidadGuardada) {
                                                                // Obtener cantidades para validación
                                                                val cantidadIngresada = cantidadEscaneo.toDoubleOrNull() ?: 0.0
                                                                val cantidadSolicitadaItem = ubicacionesDelArticulo.firstOrNull()?.requerido ?: 0.0
                                                                val saldoDisponibleUbicacion = ubicacionesDelArticulo.getOrNull(indice)?.saldoDisponible ?: 0.0
                                                                
                                                                // Validar si la cantidad excede tanto la solicitada como la disponible
                                                                val cantidadExcedida = cantidadIngresada > cantidadSolicitadaItem || cantidadIngresada > saldoDisponibleUbicacion
                                                                
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    OutlinedTextField(
                                                                        value = cantidadEscaneo,
                                                                        onValueChange = { newValue ->
                                                                            // Permitir dígitos y un solo punto decimal
                                                                            val filteredValue = buildString {
                                                                                var hasDot = false
                                                                                for (c in newValue) {
                                                                                    when {
                                                                                        c.isDigit() -> append(c)
                                                                                        c == '.' && !hasDot -> { append(c); hasDot = true }
                                                                                    }
                                                                                }
                                                                            }
                                                                            onCantidadChange(codArticulo, indice, filteredValue)
                                                                        },
                                                                        label = { Text("Cantidad", color = if (cantidadExcedida) Color.Red else Color.Black) },
                                                                        singleLine = true,
                                                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                                        modifier = Modifier.weight(1f),
                                                                        textStyle = LocalTextStyle.current.copy(color = if (cantidadExcedida) Color.Red else Color.Black),
                                                                        colors = OutlinedTextFieldDefaults.colors(
                                                                            cursorColor = if (cantidadExcedida) Color.Red else Color.Black,
                                                                            focusedBorderColor = if (cantidadExcedida) Color.Red else Color.Black,
                                                                            unfocusedBorderColor = if (cantidadExcedida) Color.Red else Color.Black,
                                                                            focusedLabelColor = if (cantidadExcedida) Color.Red else Color.Black,
                                                                            unfocusedLabelColor = if (cantidadExcedida) Color.Red else Color.Black
                                                                        ),
                                                                        placeholder = { Text("Ingrese la cantidad", color = Color.Gray) }
                                                                    )
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    IconButton(
                                                                        onClick = {
                                                                            if (cantidadEscaneo.isNotEmpty() && !cantidadExcedida) {
                                                                                onGuardarRenglon(codArticulo, indice, true)
                                                                            }
                                                                        },
                                                                        enabled = cantidadEscaneo.isNotEmpty() && !cantidadExcedida
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Filled.Save,
                                                                            contentDescription = "Guardar cantidad",
                                                                            tint = if (cantidadEscaneo.isNotEmpty() && !cantidadExcedida) Color(0xFF4CAF50) else Color.Gray,
                                                                            modifier = Modifier.size(24.dp)
                                                                        )
                                                                    }
                                                                }
                                                                
                                                                // Mostrar mensaje de error si la cantidad está excedida
                                                                if (cantidadExcedida && cantidadEscaneo.isNotEmpty()) {
                                                                    Spacer(modifier = Modifier.height(4.dp))
                                                                    Text(
                                                                        text = "La cantidad excede lo solicitado (${if (cantidadSolicitadaItem % 1.0 == 0.0) cantidadSolicitadaItem.toInt().toString() else cantidadSolicitadaItem.toString()}) o el saldo disponible (${if (saldoDisponibleUbicacion % 1.0 == 0.0) saldoDisponibleUbicacion.toInt().toString() else saldoDisponibleUbicacion.toString()})",
                                                                        color = Color.Red,
                                                                        fontSize = 12.sp,
                                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    )
                                                                }
                                                            } else {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    OutlinedTextField(
                                                                        value = cantidadEscaneo,
                                                                        onValueChange = { },
                                                                        label = { Text("Cantidad:", color = Color.Black) },
                                                                        singleLine = true,
                                                                        modifier = Modifier.weight(1f),
                                                                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                                        colors = OutlinedTextFieldDefaults.colors(
                                                                            cursorColor = Color.Black,
                                                                            focusedBorderColor = Color.Black,
                                                                            unfocusedBorderColor = Color.Black,
                                                                            focusedLabelColor = Color.Black,
                                                                            unfocusedLabelColor = Color.Black
                                                                        ),
                                                                        enabled = false,
                                                                        readOnly = true
                                                                    )
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    IconButton(
                                                                        onClick = {
                                                                            onEditarCantidad(codArticulo, indice)
                                                                        }
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Filled.Edit,
                                                                            contentDescription = "Editar cantidad",
                                                                            tint = Color.Black,
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Botón "+" para agregar nuevo escaneo
                                        // Condiciones:
                                        // 1. Hay múltiples ubicaciones disponibles
                                        // 2. La cantidad total recolectada es menor a la solicitada
                                        // 3. Aún hay ubicaciones sin asignar
                                        // 4. El último escaneo tiene partida, ubicación y cantidad guardada
                                        val ultimoIndice = listaEscaneos.lastIndex
                                        val ultimoEscaneo = if (ultimoIndice >= 0) listaEscaneos[ultimoIndice] else null
                                        val ultimoEscaneoCompleto = if (ultimoEscaneo != null) {
                                            val partidaLlena = ultimoEscaneo.partida.isNotEmpty()
                                            val ubicacionLlena = ultimoEscaneo.ubicacion.isNotEmpty()
                                            val cantidadGuardadaUltimo = cantidadesGuardadasArticulo[ultimoIndice] ?: false
                                            partidaLlena && ubicacionLlena && cantidadGuardadaUltimo
                                        } else {
                                            false
                                        }
                                        
                                        if (ubicacionesDelArticulo.size > 1 && 
                                            totalRecolectado < cantidadSolicitada && 
                                            listaEscaneos.size < ubicacionesDelArticulo.size &&
                                            ultimoEscaneoCompleto) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    // Agregar una ubicación más para este artículo
                                                    onAsegurarEscaneos(codArticulo, listaEscaneos.size + 1)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF4CAF50)
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Add,
                                                        contentDescription = "Agregar escaneo",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Agregar Partida/Ubicación",
                                                        color = Color.White,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                        

                                    }
                                }
                            }
                
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Botón enviar - validación basada en items escaneados y número de pedido
            // Para validar el envío alcanza con las ubicaciones ya cargadas en el estado.
            val ubicacionesParsed = ubicaciones.groupBy { it.codArticulo }

            // Verificar si todos los items escaneados están completos y hay número de pedido
            // Se permite envío con cargas parciales, solo se valida que los renglones existentes estén completos
            val todasCompletas = ubicacionesParsed.all { (codArticulo, ubicacionesDelArticulo) ->
                val listaEscaneos = scaneoIndividual[codArticulo] ?: emptyList()
                val cantidadesArticulo = cantidadesPorArticulo[codArticulo] ?: emptyMap()
                val cantidadesGuardadasArticulo = cantidadesGuardadas[codArticulo] ?: emptyMap()
                
                Log.d("RecolectarScreen", "=== Validando artículo: $codArticulo ===")
                Log.d("RecolectarScreen", "Lista escaneos: ${listaEscaneos.size}")
                
                // Verificar que haya al menos un escaneo y que TODOS los escaneos de este artículo estén completos
                // (con partida, ubicación, cantidad guardada Y DATOS CORRECTOS)
                val hayEscaneos = listaEscaneos.isNotEmpty()
                val todosLosEscaneosCompletos = listaEscaneos.all { escaneo ->
                    val indice = listaEscaneos.indexOf(escaneo)
                    val guardado = cantidadesGuardadasArticulo[indice] ?: false
                    val partida: String? = escaneo.partida.takeIf { it.isNotEmpty() }
                    val ubicacion: String? = escaneo.ubicacion.takeIf { it.isNotEmpty() }
                    val tieneCantidad = cantidadesArticulo[indice]?.isNotEmpty() == true
                    
                    // Validar que la cantidad no exceda tanto la solicitada como la disponible
                    val cantidadIngresada = cantidadesArticulo[indice]?.toDoubleOrNull() ?: 0.0
                    val cantidadSolicitada = ubicacionesDelArticulo.firstOrNull()?.requerido ?: 0.0
                    val saldoDisponible = ubicacionesDelArticulo.getOrNull(indice)?.saldoDisponible ?: 0.0
                    val cantidadValida = !(cantidadIngresada > cantidadSolicitada && cantidadIngresada > saldoDisponible)
                    
                    // Validar que los datos sean correctos
                    val ubicacionAsignada = ubicacionesDelArticulo.getOrNull(indice)
                    
                    // Log para debug
                    Log.d("RecolectarScreen", "ubicacionAsignada para validación: $ubicacionAsignada")
                    
                    val nombreUbicacionEsperado = ubicacionAsignada?.nombreUbicacion
                    val nroPartidaEsperado = ubicacionAsignada?.nroPartida
                    
                    Log.d("RecolectarScreen", "nombreUbicacionEsperado final: '$nombreUbicacionEsperado'")
                    Log.d("RecolectarScreen", "ubicacion escaneada: '$ubicacion'")
                    
                    val ubicacionValida = ubicacion == nombreUbicacionEsperado
                    
                    // Validar partida: si no hay partida esperada (null o "N/A"), se acepta cualquier valor o vacío
                    val partidaRequerida = nroPartidaEsperado != null && nroPartidaEsperado != "N/A"
                    val partidaValida = if (partidaRequerida) {
                        // Si hay partida esperada, debe coincidir exactamente
                        !partida.isNullOrEmpty() && partida == nroPartidaEsperado
                    } else {
                        // Si no hay partida esperada, siempre es válido (puede estar vacía o con cualquier valor)
                        true
                    }
                    
                    val resultado = !ubicacion.isNullOrEmpty() && tieneCantidad && guardado && ubicacionValida && partidaValida && cantidadValida
                    
                    Log.d("RecolectarScreen", "Escaneo[$indice]:")
                    Log.d("RecolectarScreen", "  partida=$partida, esperado=$nroPartidaEsperado, requerida=$partidaRequerida, válida=$partidaValida")
                    Log.d("RecolectarScreen", "  ubicacion=$ubicacion, esperado=$nombreUbicacionEsperado, válida=$ubicacionValida")
                    Log.d("RecolectarScreen", "  tieneCantidad=$tieneCantidad, guardado=$guardado")
                    Log.d("RecolectarScreen", "  cantidad=$cantidadIngresada, solicitada=$cantidadSolicitada, disponible=$saldoDisponible, válida=$cantidadValida")
                    Log.d("RecolectarScreen", "  RESULTADO=$resultado")
                    
                    resultado
                }
                
                // Calcular la suma total de cantidades guardadas para este artículo
                val cantidadTotalRecolectada = listaEscaneos.indices.sumOf { indice ->
                    val guardado = cantidadesGuardadasArticulo[indice] ?: false
                    if (guardado) {
                        cantidadesArticulo[indice]?.toDoubleOrNull() ?: 0.0
                    } else {
                        0.0
                    }
                }
                
                val cantidadSolicitadaTotal = ubicacionesDelArticulo.firstOrNull()?.requerido ?: 0.0
                val cantidadSuficiente = cantidadTotalRecolectada >= cantidadSolicitadaTotal
                
                val resultadoArticulo = hayEscaneos && todosLosEscaneosCompletos && cantidadSuficiente
                Log.d("RecolectarScreen", "Artículo $codArticulo: hayEscaneos=$hayEscaneos, completos=$todosLosEscaneosCompletos, cantidadRecolectada=$cantidadTotalRecolectada, cantidadSolicitada=$cantidadSolicitadaTotal, suficiente=$cantidadSuficiente, resultado=$resultadoArticulo")
                resultadoArticulo
            }

            val tienePedido = idPedido > 0
            val datosListos = !isLoadingUbicaciones && errorUbicaciones == null && ubicaciones.isNotEmpty()


            if (todasCompletas && tienePedido && datosListos) {
                Button(
                    onClick = onEnviar,
                    enabled = !isLoadingEnvio,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFFCCCCCC),
                        disabledContentColor = Color(0xFF666666)
                    )
                ) {
                    if (isLoadingEnvio) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Enviar recolección",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar Recolección", color = Color.Black, fontSize = 16.sp)
                        }
                    }
                }
                
                Log.d("RecolectarScreen", "Estado del botón enviar: HABILITADO")
                Log.d("RecolectarScreen", "Todas las ubicaciones completas: $todasCompletas")
                Log.d("RecolectarScreen", "Tiene pedido: $tienePedido")
                Log.d("RecolectarScreen", "Datos listos (no cargando): $datosListos")
            } else {
                Log.d("RecolectarScreen", "Estado del botón enviar: DESHABILITADO")
                Log.d("RecolectarScreen", "Todas las ubicaciones completas: $todasCompletas")
                Log.d("RecolectarScreen", "Tiene pedido: $tienePedido")
                Log.d("RecolectarScreen", "Datos listos (no cargando): $datosListos")
                
                // Debug individual de cada artículo
                ubicacionesParsed.forEach { (codArticulo, _) ->
                    val listaEscaneos = scaneoIndividual[codArticulo] ?: emptyList()
                    val cantidadesArticulo = cantidadesPorArticulo[codArticulo] ?: emptyMap()
                    val cantidadesGuardadasArticulo = cantidadesGuardadas[codArticulo] ?: emptyMap()
                    
                    val totalRecolectado = listaEscaneos.indices.sumOf { indice ->
                        val guardado = cantidadesGuardadasArticulo[indice] ?: false
                        if (guardado) {
                            cantidadesArticulo[indice]?.toIntOrNull() ?: 0
                        } else {
                            0
                        }
                    }
                    
                    Log.d("RecolectarScreen", "Debug artículo $codArticulo:")
                    Log.d("RecolectarScreen", "  - Escaneos: ${listaEscaneos.size}")
                    Log.d("RecolectarScreen", "  - Total recolectado: $totalRecolectado")
                }
            }

            // Mostrar error de envío si existe
            if (errorEnvio != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ErrorMessage(
                    message = errorEnvio!!,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    onDismiss = { /* errorEnvio se limpia automáticamente */ }
                )
            }
            }
        }
        
        // Diálogo de confirmación para regresar
        if (showBackDialog) {
            AlertDialog(

                containerColor = Color.White,
                onDismissRequest = { showBackDialog = false },
                title = {
                    Text(
                        text = "Confirmar regreso",
                        color = Color.Black,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "¿Está seguro que desea regresar? Se perderán los datos no guardados.",
                        color = Color.Black
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBackDialog = false
                            onBack()
                        }
                    ) {
                        Text("Sí, regresar", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showBackDialog = false }
                    ) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }

        // Diálogo de confirmación para ir a main
        if (showCloseDialog) {
            AlertDialog(
                containerColor = Color.White,
                onDismissRequest = { showCloseDialog = false },
                title = {
                    Text(
                        text = "Confirmar salida",
                        color = Color.Black,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "¿Está seguro que desea ir al menú principal? Se perderán los datos no guardados.",
                        color = Color.Black
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCloseDialog = false
                            onClose()
                        }
                    ) {
                        Text("Sí, ir al menú", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCloseDialog = false }
                    ) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
        
        // Diálogo de confirmación para eliminar renglón
        if (showDeleteRenglonDialog && renglonAEliminar != null) {
            AlertDialog(
                containerColor = Color.White,
                onDismissRequest = { 
                    showDeleteRenglonDialog = false
                    renglonAEliminar = null
                },
                title = {
                    Text(
                        text = "Confirmar eliminación",
                        color = Color.Black,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "¿Está seguro que desea eliminar este renglón? Esta acción no se podrá recuperar.",
                        color = Color.Black
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val (codArticulo, indice) = renglonAEliminar!!

                            // El borrado en base y la reindexación los resuelve el ViewModel
                            onEliminarRenglon(codArticulo, indice)

                            
                            // Eliminar campos editables
                            val editablesArt = camposEditables[codArticulo]?.toMutableMap() ?: mutableMapOf()
                            editablesArt.remove(indice)
                            // Reindexar campos editables
                            val nuevosEditables = mutableMapOf<Int, Map<String, Boolean>>()
                            editablesArt.entries.sortedBy { it.key }.forEachIndexed { newIndex, entry ->
                                if (entry.key > indice) {
                                    nuevosEditables[newIndex] = entry.value
                                } else if (entry.key < indice) {
                                    nuevosEditables[entry.key] = entry.value
                                }
                            }
                            camposEditables = camposEditables + (codArticulo to nuevosEditables)
                            
                            showDeleteRenglonDialog = false
                            renglonAEliminar = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Red
                        )
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showDeleteRenglonDialog = false
                            renglonAEliminar = null
                        }
                    ) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }
    
    // Mostrar pantalla de éxito cuando se complete la recolección
    if (showSuccessScreen) {
        RecolectarSuccessScreen(
            onFinish = onSuccess
        )
    }
}


@Preview(showBackground = true, name = "RecolectarScreen - Cargando")
@Composable
fun PreviewRecolectarScreenLoading() {
    BarCodeScannerTheme {
        RecolectarScreen(state = RecolectarUiState(idPedido = 1234, isLoadingUbicaciones = true))
    }
}

@Preview(showBackground = true, name = "RecolectarScreen - Error")
@Composable
fun PreviewRecolectarScreenError() {
    BarCodeScannerTheme {
        RecolectarScreen(
            state = RecolectarUiState(
                idPedido = 1234,
                errorUbicaciones = "No se pudo cargar ubicaciones"
            )
        )
    }
}

@Preview(showBackground = true, name = "RecolectarScreen - Ubicaciones")
@Composable
fun PreviewRecolectarScreenUbicaciones() {
    BarCodeScannerTheme {
        RecolectarScreen(
            state = RecolectarUiState(
                idPedido = 1234,
                ubicaciones = listOf(
                    UbicacionRecolectar(
                        numeroUbicacion = "001",
                        nombreUbicacion = "A-01",
                        descripcionArticulo = "Artículo de prueba",
                        codArticulo = "ART123",
                        requerido = 10.0,
                        saldoDisponible = 6.0,
                        nroPartida = "P-1"
                    ),
                    UbicacionRecolectar(
                        numeroUbicacion = "002",
                        nombreUbicacion = "B-02",
                        descripcionArticulo = "Artículo de prueba",
                        codArticulo = "ART123",
                        requerido = 10.0,
                        saldoDisponible = 5.0,
                        nroPartida = "P-2"
                    )
                )
            )
        )
    }
}
