package com.thinkthat.mamusckascaner.view

import android.content.Context
import android.os.Debug
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinkthat.mamusckascaner.model.RecolectarItem
import com.thinkthat.mamusckascaner.service.Services.ApiClient
import com.thinkthat.mamusckascaner.view.components.ErrorMessage
import com.thinkthat.mamusckascaner.view.components.LoadingMessage
import com.thinkthat.mamusckascaner.view.components.SuccessMessage
import com.thinkthat.mamusckascaner.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject

import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import com.thinkthat.mamusckascaner.utils.QRData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecolectarScreen(
    onBack: () -> Unit = {},
    onClose: () -> Unit = {},
    onStockearClick: (boton: String) -> Unit = {},
    partida: String? = null,
    ubicacion: String? = null,
    ordenCompleta: com.thinkthat.mamusckascaner.service.Services.OrdenTrabajoCompleta? = null,
    isLoadingOrden: Boolean = false,
    errorOrden: String? = null,
    ordenTrabajo: com.thinkthat.mamusckascaner.model.OrdenTrabajo? = null, // Mantener compatibilidad
    ubicacionesRecolectar: String? = null,
    isLoadingUbicaciones: Boolean = false,
    errorUbicaciones: String? = null,
    qrData: QRData? = null,
    fromQR: Boolean = false,
    onRetryUbicaciones: () -> Unit = {},
    onSuccess: () -> Unit = {},
    onClearScanValues: () -> Unit = {}
) {
    val context = LocalContext.current
    var scanStep by remember { mutableStateOf("partida") }
    var partidaActual by remember { mutableStateOf<String?>(null) }
    var ubicacionActual by remember { mutableStateOf<String?>(null) }

    var deposito by remember { 
        mutableStateOf(if (fromQR && qrData != null) qrData.deposito else "") 
    }
    var errorEnvio by remember { mutableStateOf<String?>(null) }
    var mensajeExito by remember { mutableStateOf<String?>(null) }
    var showSuccessScreen by remember { mutableStateOf(false) }
    var isLoadingEnvio by remember { mutableStateOf(false) }
    
    // Estado para diálogos de confirmación
    var showBackDialog by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }
    
    // Estado para escaneo individual por codArticulo
    var scaneoIndividual by remember { mutableStateOf(mapOf<String, Map<String, String>>()) }
    var articuloActualEscaneando by remember { mutableStateOf<String?>(null) }
    var tipoEscaneoActual by remember { mutableStateOf<String?>(null) }
    
    // Estado para cantidades por artículo
    var cantidadesPorArticulo by remember { mutableStateOf(mapOf<String, String>()) }
    
    // Estado para campos editables por artículo
    var camposEditables by remember { mutableStateOf(mapOf<String, Map<String, Boolean>>()) }
    
    // Estado para cantidades guardadas por artículo
    var cantidadesGuardadas by remember { mutableStateOf(mapOf<String, Boolean>()) }

    // Manejo de escaneo (individual por ítem). Cada botón establece articuloActualEscaneando y tipoEscaneoActual
    // El valor escaneado SIEMPRE debe sobrescribir (override) el existente del mismo ítem y solo de ese ítem.
    // Incluimos las banderas en la key para permitir re-escaneo con el mismo valor (p.ej. escanear de nuevo tras editar manualmente)
    LaunchedEffect(partida, articuloActualEscaneando, tipoEscaneoActual) {
        if (partida.isNullOrBlank()) return@LaunchedEffect

        // Flujo legacy secuencial (solo si se usa aún scanStep global). No afecta la lógica individual.
        if (scanStep == "partida" && articuloActualEscaneando == null && tipoEscaneoActual == null) {
            partidaActual = partida
            scanStep = "ubicacion"
        }

        if (articuloActualEscaneando != null && tipoEscaneoActual == "partida") {
            val articuloId = articuloActualEscaneando!!
            val datosPrevios = scaneoIndividual[articuloId] ?: emptyMap()
            val nuevoMapa = datosPrevios + ("partida" to partida)
            scaneoIndividual = scaneoIndividual + (articuloId to nuevoMapa)

            // Forzamos el campo a editable para visualizar inmediato el valor escaneado
            val camposPrevios = camposEditables[articuloId] ?: mapOf("partida" to false, "ubicacion" to false)
            camposEditables = camposEditables + (articuloId to (camposPrevios + ("partida" to true)))


            // Limpiar banderas de escaneo para permitir próximos escaneos
            tipoEscaneoActual = null
            articuloActualEscaneando = null
            // Limpiar valor escaneado de la memoria
            onClearScanValues()
        } else {
            Log.d("RecolectarScreen", "[SCAN-IGNORED] Partida recibido pero sin target válido (articuloActualEscaneando=$articuloActualEscaneando tipoEscaneoActual=$tipoEscaneoActual)")
        }
    }

    LaunchedEffect(ubicacion, articuloActualEscaneando, tipoEscaneoActual) {
        if (ubicacion.isNullOrBlank()) return@LaunchedEffect

        // Flujo legacy secuencial global
        if (scanStep == "ubicacion" && articuloActualEscaneando == null && tipoEscaneoActual == null) {
            ubicacionActual = ubicacion
            scanStep = "cantidad"
        }

        if (articuloActualEscaneando != null && tipoEscaneoActual == "ubicacion") {
            val articuloId = articuloActualEscaneando!!
            val datosPrevios = scaneoIndividual[articuloId] ?: emptyMap()
            val nuevoMapa = datosPrevios + ("ubicacion" to ubicacion)
            scaneoIndividual = scaneoIndividual + (articuloId to nuevoMapa)

            val camposPrevios = camposEditables[articuloId] ?: mapOf("partida" to false, "ubicacion" to false)
            camposEditables = camposEditables + (articuloId to (camposPrevios + ("ubicacion" to true)))

            Log.d("RecolectarScreen", "[SCAN] Ubicacion asignada -> articulo=$articuloId valor=$ubicacion")
            tipoEscaneoActual = null
            articuloActualEscaneando = null
            // Limpiar valor escaneado de la memoria
            onClearScanValues()
        } else {
            Log.d("RecolectarScreen", "[SCAN-IGNORED] Ubicación recibida pero sin target válido (articuloActualEscaneando=$articuloActualEscaneando tipoEscaneoActual=$tipoEscaneoActual)")
        }
    }

    // Auto-ocultar mensaje de éxito después de 3 segundos
    LaunchedEffect(mensajeExito) {
        if (mensajeExito != null) {
            kotlinx.coroutines.delay(3000)
            mensajeExito = null
        }
    }

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
                        modifier = Modifier.fillMaxWidth()
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

            // Mostrar mensaje de éxito si existe
            if (mensajeExito != null) {
                SuccessMessage(
                    message = mensajeExito!!,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }

            // Información del QR o campo depósito
            if (fromQR && qrData != null) {
                // Mostrar información del QR
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
                        if (qrData.deposito.isNotEmpty()) {
                            Text(
                                text = "Depósito: ${qrData.deposito}",
                                fontSize = 16.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        if (qrData.pedido.isNotEmpty()) {
                            Text(
                                text = "Pedido: ${qrData.pedido}",
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            } else {
                // Campo depósito tradicional
                OutlinedTextField(
                    value = deposito,
                    onValueChange = { deposito = it },
                    label = { Text("Depósito", color = Color.Black) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        //textColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedBorderColor = Color(0xFF1976D2),
                        unfocusedBorderColor = Color(0xFF1976D2),
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.Black
                    )
                )
            }

            // Mostrar estado de carga de ubicaciones
            if (isLoadingUbicaciones) {
                LoadingMessage(
                    message = "Cargando ubicaciones para recolectar...",
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Mostrar información de las ubicaciones para recolectar
            if (ubicacionesRecolectar != null) {
                // Parsear las ubicaciones fuera del contexto Composable
                val ubicacionesParsed = remember(ubicacionesRecolectar) {
                    try {
                        // Intentar parsear como JSON Array
                        val jsonArray = JSONArray(ubicacionesRecolectar)
                        val ubicaciones = mutableListOf<Map<String, Any>>()
                        for (i in 0 until jsonArray.length()) {
                            val ubicacion = jsonArray.getJSONObject(i)
                            val numeroUbicacion = ubicacion.optString("numero", "N/A")
                            
                            // Obtener el array de artículos
                            val articulosArray = ubicacion.optJSONArray("articulos")
                            
                            if (articulosArray != null && articulosArray.length() > 0) {
                                // Por cada artículo, agregar una entrada a ubicaciones
                                for (j in 0 until articulosArray.length()) {
                                    val articulo = articulosArray.getJSONObject(j)
                                    val nombreUbicacion = ubicacion.optString("nombre", "N/A")
                                    ubicaciones.add(mapOf(
                                        "numeroUbicacion" to numeroUbicacion,
                                        "nombreUbicacion" to nombreUbicacion,
                                        "descripcionPartida" to articulo.optString("descripcion", "N/A"),
                                        "codArticulo" to articulo.optString("codigo", "N/A"),
                                        "requerido" to articulo.optInt("requerido", 0)
                                    ))
                                }
                            } 
                            // else {
                            //     // Si no hay artículos, agregar la ubicación sin artículo
                            //     ubicaciones.add(mapOf(
                            //         "numero" to numeroUbicacion,
                            //         "nombre" to nombreUbicacion,
                            //         "codArticulo" to "N/A"
                            //     ))
                            // }
                        }
                        Pair("array", ubicaciones)
                    } catch (e: Exception) {
                        // Si no es JSON Array, intentar como JSON Object
                        try {
                            val jsonObject = JSONObject(ubicacionesRecolectar)
                            Pair("object", jsonObject.toString(2))
                        } catch (e2: Exception) {
                            // Si no es JSON válido, mostrar como texto plano
                            Pair("text", ubicacionesRecolectar)
                        }
                    }
                }
                
               

                    
                    when (ubicacionesParsed.first) {
                        "array" -> {
                           
                            val ubicaciones = ubicacionesParsed.second as List<Map<String, Any>>
                            
                            // Agrupar por codArticulo
                            val ubicacionesGrouped = ubicaciones.groupBy { it["codArticulo"] as String }
                            
                            // Estado para dropdowns expandidos
                            var expandedItems by remember { mutableStateOf(setOf<String>()) }
                            
                            ubicacionesGrouped.forEach { (codArticulo, ubicacionesDelArticulo) ->
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
                                        val descripcionArticulo = ubicacionesDelArticulo.firstOrNull()?.get("descripcionPartida") as? String ?: "N/A"
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
                                        
                                        // Mostrar cantidad solicitada
                                        val cantidadSolicitada = ubicacionesDelArticulo.firstOrNull()?.get("requerido") as? Int ?: 0
                                        Text(
                                            text = "Solicitado: $cantidadSolicitada",
                                            color = Color.Black,
                                            fontSize = 14.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Estado del escaneo para este artículo
                                        val datosEscaneo = scaneoIndividual[codArticulo] ?: mapOf()
                                        val partidaEscaneado = datosEscaneo["partida"]
                                        val ubicacionEscaneada = datosEscaneo["ubicacion"]
                                        val cantidadArticulo = cantidadesPorArticulo[codArticulo] ?: ""
                                        
                                        // Estado de campos editables para este artículo
                                        val camposArticulo = camposEditables[codArticulo] ?: mapOf("partida" to false, "ubicacion" to false)
                                        val partidaEditable = camposArticulo["partida"] ?: false
                                        val ubicacionEditable = camposArticulo["ubicacion"] ?: false
                                        
                                        // Estado de cantidad guardada para este artículo
                                        val cantidadGuardada = cantidadesGuardadas[codArticulo] ?: false
                                        
                                        // Auto-llenar cantidad con requerido cuando partida y ubicación estén completos
                                        LaunchedEffect(partidaEscaneado, ubicacionEscaneada, cantidadSolicitada) {
                                            if (partidaEscaneado?.isNotEmpty() == true && 
                                                ubicacionEscaneada?.isNotEmpty() == true &&
                                                cantidadArticulo.isEmpty() &&
                                                cantidadSolicitada > 0) {
                                                cantidadesPorArticulo = cantidadesPorArticulo + (codArticulo to cantidadSolicitada.toString())
                                            }
                                        }
                                        
                                        // Campos y botones secuenciales
                                        // Lógica secuencial: mostrar campos y botones según el estado
                                        if (partidaEscaneado?.isEmpty() != false) {
                                            // 1. Mostrar campo partida + botón escanear partida
                                            if (!partidaEditable) {
                                                // Modo solo lectura - sin bordes, texto negro
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
                                                            val articuloId = codArticulo
                                                            val camposActuales = camposEditables[articuloId] ?: mapOf("partida" to false, "ubicacion" to false)
                                                            camposEditables = camposEditables + (articuloId to (camposActuales + ("partida" to true)))
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
                                                // Modo edición - campo normal con ícono de guardar
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    OutlinedTextField(
                                                        value = partidaEscaneado ?: "",
                                                        onValueChange = { newValue ->
                                                            val articuloId = codArticulo
                                                            val datosActuales = scaneoIndividual[articuloId] ?: mapOf()
                                                            scaneoIndividual = scaneoIndividual + (articuloId to (datosActuales + ("partida" to newValue)))
                                                        },
                                                        label = { Text("Partida", color = Color.Black) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                                            cursorColor = Color.Black,
                                                            focusedBorderColor = Color.Black,
                                                            unfocusedBorderColor = Color.Black,
                                                            focusedLabelColor = Color.Black,
                                                            unfocusedLabelColor = Color.Black
                                                        ),
                                                        placeholder = { Text("Código del partida", color = Color.Gray) }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = {
                                                            val articuloId = codArticulo
                                                            val camposActuales = camposEditables[articuloId] ?: mapOf("partida" to false, "ubicacion" to false)
                                                            camposEditables = camposEditables + (articuloId to (camposActuales + ("partida" to false)))
                                                        }
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Save,
                                                            contentDescription = "Guardar partida",
                                                            tint = if (partidaEscaneado != "" ) Color(0xFF4CAF50) else Color.Gray,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Button(
                                                onClick = {
                                                    articuloActualEscaneando = codArticulo
                                                    tipoEscaneoActual = "partida"
                                                    onStockearClick("partida")
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
                                        } else if (ubicacionEscaneada?.isEmpty() != false) {
                                            // 2. Mostrar campo partida (ya escaneado) + campo ubicación + botón escanear ubicación
                                            if (!partidaEditable) {
                                                // Modo solo lectura - sin bordes
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
                                                            text = "Partida:\n${partidaEscaneado ?: ""}",
                                                            color = Color.Black,
                                                            fontSize = 16.sp
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            articuloActualEscaneando = codArticulo
                                                            tipoEscaneoActual = "partida"
                                                            onStockearClick("partida")
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
                                                            val articuloId = codArticulo
                                                            val camposActuales = camposEditables[articuloId] ?: mapOf("partida" to false, "ubicacion" to false)
                                                            camposEditables = camposEditables + (articuloId to (camposActuales + ("partida" to true)))
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
                                                // Modo edición
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    OutlinedTextField(
                                                        value = partidaEscaneado ?: "",
                                                        onValueChange = { newValue ->
                                                            val articuloId = codArticulo
                                                            val datosActuales = scaneoIndividual[articuloId] ?: mapOf()
                                                            scaneoIndividual = scaneoIndividual + (articuloId to (datosActuales + ("partida" to newValue)))
                                                        },
                                                        label = { Text("Partida", color = Color.Black) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                                            cursorColor = Color.Black,
                                                            focusedBorderColor = Color.Black,
                                                            unfocusedBorderColor = Color.Black,
                                                            focusedLabelColor = Color.Black,
                                                            unfocusedLabelColor = Color.Black
                                                        ),
                                                        placeholder = { Text("Numero del Partida", color = Color.Gray) }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = {
                                                            val articuloId = codArticulo
                                                            val camposActuales = camposEditables[articuloId] ?: mapOf("partida" to false, "ubicacion" to false)
                                                            camposEditables = camposEditables + (articuloId to (camposActuales + ("partida" to false)))
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
                                            
                                            if (!ubicacionEditable) {
                                                // Modo solo lectura - sin bordes
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
                                                            val articuloId = codArticulo
                                                            val camposActuales = camposEditables[articuloId] ?: mapOf("partida" to false, "ubicacion" to false)
                                                            camposEditables = camposEditables + (articuloId to (camposActuales + ("ubicacion" to true)))
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
                                                // Modo edición
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    OutlinedTextField(
                                                        value = ubicacionEscaneada ?: "",
                                                        onValueChange = { newValue ->
                                                            val articuloId = codArticulo
                                                            val datosActuales = scaneoIndividual[articuloId] ?: mapOf()
                                                            scaneoIndividual = scaneoIndividual + (articuloId to (datosActuales + ("ubicacion" to newValue)))
                                                        },
                                                        label = { Text("Ubicación", color = Color.Black) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                                            cursorColor = Color.Black,
                                                            focusedBorderColor = Color.Black,
                                                            unfocusedBorderColor = Color.Black,
                                                            focusedLabelColor = Color.Black,
                                                            unfocusedLabelColor = Color.Black
                                                        ),
                                                        placeholder = { Text("Código de ubicación", color = Color.Gray) }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = {
                                                            val articuloId = codArticulo
                                                            val camposActuales = camposEditables[articuloId] ?: mapOf("partida" to false, "ubicacion" to false)
                                                            camposEditables = camposEditables + (articuloId to (camposActuales + ("ubicacion" to false)))
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
                                                    articuloActualEscaneando = codArticulo
                                                    tipoEscaneoActual = "ubicacion"
                                                    onStockearClick("ubicacion")
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
                                            // 3. Mostrar ambos campos (ya escaneados) + botones de reescaneo
                                            if (!partidaEditable) {
                                                // Modo solo lectura - sin bordes
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
                                                            text = "Partida:\n ${partidaEscaneado ?: ""}",
                                                            color = Color.Black,
                                                            fontSize = 16.sp
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            articuloActualEscaneando = codArticulo
                                                            tipoEscaneoActual = "partida"
                                                            onStockearClick("partida")
                                                        }
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.CameraAlt,
                                                            contentDescription = "Escanear Partida",
                                                            tint = Color.Black,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val articuloId = codArticulo
                                                            val camposActuales = camposEditables[articuloId] ?: mapOf("partida" to false, "ubicacion" to false)
                                                            camposEditables = camposEditables + (articuloId to (camposActuales + ("partida" to true)))
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
                                                // Modo edición
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    OutlinedTextField(
                                                        value = partidaEscaneado ?: "",
                                                        onValueChange = { newValue ->
                                                            val articuloId = codArticulo
                                                            val datosActuales = scaneoIndividual[articuloId] ?: mapOf()
                                                            scaneoIndividual = scaneoIndividual + (articuloId to (datosActuales + ("partida" to newValue)))
                                                        },
                                                        label = { Text("partida", color = Color.Black) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                                            cursorColor = Color.Black,
                                                            focusedBorderColor = Color.Black,
                                                            unfocusedBorderColor = Color.Black,
                                                            focusedLabelColor = Color.Black,
                                                            unfocusedLabelColor = Color.Black
                                                        ),
                                                        placeholder = { Text("Numero del partida", color = Color.Gray) }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = {
                                                            val articuloId = codArticulo
                                                            val camposActuales = camposEditables[articuloId] ?: mapOf("partida" to false, "ubicacion" to false)
                                                            camposEditables = camposEditables + (articuloId to (camposActuales + ("partida" to false)))
                                                        }
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Save,
                                                            contentDescription = "Guardar partida",
                                                            tint = if (partidaEscaneado.isNotEmpty()) Color(0xFF4CAF50) else Color.Gray,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            if (!ubicacionEditable) {
                                                // Modo solo lectura - sin bordes
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
                                                            text = "Ubicación:\n ${ubicacionEscaneada ?: ""}",
                                                            color = Color.Black,
                                                            fontSize = 16.sp
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            articuloActualEscaneando = codArticulo
                                                            tipoEscaneoActual = "ubicacion"
                                                            onStockearClick("ubicacion")
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
                                                            val articuloId = codArticulo
                                                            val camposActuales = camposEditables[articuloId] ?: mapOf("partida" to false, "ubicacion" to false)
                                                            camposEditables = camposEditables + (articuloId to (camposActuales + ("ubicacion" to true)))
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
                                                // Modo edición
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    OutlinedTextField(
                                                        value = ubicacionEscaneada ?: "",
                                                        onValueChange = { newValue ->
                                                            val articuloId = codArticulo
                                                            val datosActuales = scaneoIndividual[articuloId] ?: mapOf()
                                                            scaneoIndividual = scaneoIndividual + (articuloId to (datosActuales + ("ubicacion" to newValue)))
                                                        },
                                                        label = { Text("Ubicación", color = Color.Black) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                                            cursorColor = Color.Black,
                                                            focusedBorderColor = Color.Black,
                                                            unfocusedBorderColor = Color.Black,
                                                            focusedLabelColor = Color.Black,
                                                            unfocusedLabelColor = Color.Black
                                                        ),
                                                        placeholder = { Text("Código de ubicación", color = Color.Gray) }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = {
                                                            val articuloId = codArticulo
                                                            val camposActuales = camposEditables[articuloId] ?: mapOf("partida" to false, "ubicacion" to false)
                                                            camposEditables = camposEditables + (articuloId to (camposActuales + ("ubicacion" to false)))
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
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Campo cantidad (visible cuando partida y ubicación están completos)
                                        if ((partidaEscaneado?.isNotEmpty() == true) && (ubicacionEscaneada?.isNotEmpty() == true)) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            if (!cantidadGuardada) {
                                                // Mostrar campo de entrada con botón guardar
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    OutlinedTextField(
                                                        value = cantidadArticulo,
                                                        onValueChange = { newValue ->
                                                            // Solo permitir números
                                                            val filteredValue = newValue.filter { it.isDigit() }
                                                            cantidadesPorArticulo = cantidadesPorArticulo + (codArticulo to filteredValue)
                                                        },
                                                        label = { Text("Cantidad", color = Color.Black) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                                            cursorColor = Color.Black,
                                                            focusedBorderColor = Color.Black,
                                                            unfocusedBorderColor = Color.Black,
                                                            focusedLabelColor = Color.Black,
                                                            unfocusedLabelColor = Color.Black
                                                        ),
                                                        placeholder = { Text("Ingrese la cantidad", color = Color.Gray) }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = {
                                                            if (cantidadArticulo.isNotEmpty()) {
                                                                cantidadesGuardadas = cantidadesGuardadas + (codArticulo to true)
                                                            }
                                                        },
                                                        enabled = cantidadArticulo.isNotEmpty()
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Save,
                                                            contentDescription = "Guardar cantidad",
                                                            tint = if (cantidadArticulo.isNotEmpty()) Color(0xFF4CAF50) else Color.Gray,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                            } else {
                                                // Mostrar cantidad guardada con botón editar
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    OutlinedTextField(
                                                        value = cantidadArticulo,
                                                        onValueChange = { },
                                                        label = { Text("Cantidad:", color = Color.Black) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                        colors = TextFieldDefaults.outlinedTextFieldColors(
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
                                                            cantidadesGuardadas = cantidadesGuardadas + (codArticulo to false)
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
                                        
                                        //if (ubicacionesDelArticulo.size == 1) {
                                        
                                        if (true) {
                                            // Solo una ubicación - mostrar directamente
                                            val ubicacion = ubicacionesDelArticulo[0]
                                            Column {
                                                Text(
                                                    text = "Ubicación: ${ubicacion["nombreUbicacion"]}",
                                                    color = Color.Black,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        } else {
                                            // Múltiples ubicaciones - mostrar dropdown
                                            val isExpanded = expandedItems.contains(codArticulo)
                                            
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        expandedItems = if (isExpanded) {
                                                            expandedItems - codArticulo
                                                        } else {
                                                            expandedItems + codArticulo
                                                        }
                                                    }
                                                    .background(
                                                        Color(0xFFE3F2FD),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Ubicaciones (${ubicacionesDelArticulo.size})",
                                                    color = Color.Black,
                                                    fontSize = 14.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                                )
                                                Text(
                                                    text = if (isExpanded) "▼" else "▶",
                                                    color = Color.Black,
                                                    fontSize = 16.sp
                                                )
                                            }
                                            
                                            if (isExpanded) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                ubicacionesDelArticulo.forEach { ubicacion ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 2.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = Color(0xFFE8F5E8)
                                                        ),
                                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(8.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "Ubicación: ${ubicacion["nombreUbicacion"]}",
                                                                    color = Color.Black,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                                                )
                                                            }
                                                           
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        

                                    }
                                }
                            }
                        }
                        "object", "text" -> {
                            Text("Respuesta formateada:", color = Color(0xFF7B1FA2), fontSize = 14.sp)
                            Text(
                                text = ubicacionesParsed.second as String,
                                color = Color.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Botón enviar - validación basada en items escaneados y número de pedido
            val ubicacionesParsed = remember(ubicacionesRecolectar) {
                try {
                    val jsonArray = JSONArray(ubicacionesRecolectar ?: "[]")
                    Log.d("RecolectarScreen", "${jsonArray.toString()}")

                    val ubicaciones = mutableListOf<Map<String, Any>>()
                    for (i in 0 until jsonArray.length()) {
                        val ubicacion = jsonArray.getJSONObject(i)
                        val nombreUbicacion = ubicacion.optString("nombre", "N/A")
                        val numeroUbicacion = ubicacion.optString("numero", "N/A")
                        val aliasUbicacion = ubicacion.optString("alias", "N/A")
                        val ordenUbicacion = ubicacion.optInt("orden", 0)
                        
                        // Obtener el array de artículos
                        val articulosArray = ubicacion.optJSONArray("articulos")
                        
                        if (articulosArray != null && articulosArray.length() > 0) {
                            // Por cada artículo, agregar una entrada a ubicaciones
                            for (j in 0 until articulosArray.length()) {
                                val articulo = articulosArray.getJSONObject(j)
                                ubicaciones.add(mapOf(
                                    "numero" to numeroUbicacion,
                                    "nombre" to nombreUbicacion,
                                    "alias" to aliasUbicacion,
                                    "orden" to ordenUbicacion,
                                    "codArticulo" to articulo.optString("codigo", "N/A"),
                                    "codigoBarras" to articulo.optString("codigoBarras", "N/A"),
                                    "descripcion" to articulo.optString("descripcion", "N/A"),
                                    "requerido" to articulo.optInt("requerido", 0),
                                    "saldoDisponible" to articulo.optInt("saldoDisponible", 0),
                                    "usaPartidas" to articulo.optBoolean("usaPartidas", false),
                                    "usaSeries" to articulo.optBoolean("usaSeries", false)
                                ))
                            }
                        } else {
                            // Si no hay artículos, agregar la ubicación sin artículo
                            ubicaciones.add(mapOf(
                                "numero" to numeroUbicacion,
                                "nombre" to nombreUbicacion,
                                "alias" to aliasUbicacion,
                                "orden" to ordenUbicacion,
                                "codArticulo" to "N/A"
                            ))
                        }
                    }
                    ubicaciones.groupBy { it["codArticulo"] as String }
                } catch (e: Exception) {
                    mapOf<String, List<Map<String, Any>>>()
                }
            }

            // Verificar si todos los items escaneados están completos y hay número de pedido
            val todasCompletas = ubicacionesParsed.all { (codArticulo, _) ->
                val datosEscaneo = scaneoIndividual[codArticulo]
                val partida = datosEscaneo?.get("partida")
                val ubicacion = datosEscaneo?.get("ubicacion")
                val cantidad = cantidadesPorArticulo[codArticulo]
                val cantidadGuardada = cantidadesGuardadas[codArticulo] == true
                
                !partida.isNullOrEmpty() && !ubicacion.isNullOrEmpty() && 
                !cantidad.isNullOrEmpty() && cantidadGuardada
            }

            val tienePedido = qrData?.pedido?.isNotEmpty() == true
            val tieneDeposito = (qrData?.deposito?.isNotEmpty() == true) || deposito.isNotBlank()
            val datosListos = !isLoadingUbicaciones && errorUbicaciones == null && ubicacionesRecolectar != null

            if (todasCompletas && tienePedido && tieneDeposito && datosListos) {
                Button(
                    onClick = {
                        if (!isLoadingEnvio) {
                            isLoadingEnvio = true
                            errorEnvio = null
                            mensajeExito = null
                            
                            val prefs = context.getSharedPreferences("QRCodeScannerPrefs", Context.MODE_PRIVATE)
                            val usuario = prefs.getString("savedUser", "") ?: ""
                            
                            Log.d("RecolectarScreen", "=== INICIANDO ENVÍO DE RECOLECCIÓN ===")
                            
                            // Obtener el codDeposito efectivo
                            val efectivoCodDeposito = when {
                                qrData?.deposito?.isNotEmpty() == true -> qrData.deposito
                                deposito.isNotEmpty() -> deposito
                                else -> "DEFAULT_DEPOSITO"
                            }
                            
                            Log.d("RecolectarScreen", "Usuario: $usuario")
                            Log.d("RecolectarScreen", "CodDeposito efectivo: $efectivoCodDeposito")
                            Log.d("RecolectarScreen", "QR Data: $qrData")
                            Log.d("RecolectarScreen", "Deposito manual: $deposito")
                            
                            // Build JSON body con el nuevo formato
                            val json = JSONObject()
                            
                            // Obtener idPedido del QR
                            val idPedido = qrData?.pedido?.toIntOrNull() ?: 0
                            json.put("idPedido", idPedido)
                            
                            Log.d("RecolectarScreen", "ID Pedido: $idPedido")
                            
                            // Array de recolecciones con datos individuales
                            val recoleccionesArray = JSONArray()
                            
                            Log.d("RecolectarScreen", "=== DATOS DE RECOLECCIONES ===")
                            scaneoIndividual.forEach { (codArticulo, datosEscaneo) ->
                                val partida = datosEscaneo["partida"] ?: ""
                                val ubicacion = datosEscaneo["ubicacion"] ?: ""
                                val cantidad = cantidadesPorArticulo[codArticulo]?.toIntOrNull() ?: 0
                                
                                Log.d("RecolectarScreen", "Artículo: $codArticulo")
                                Log.d("RecolectarScreen", "  - Partida: $partida")
                                Log.d("RecolectarScreen", "  - Ubicación: $ubicacion")
                                Log.d("RecolectarScreen", "  - Cantidad: $cantidad")
                                Log.d("RecolectarScreen", "  - CodDeposito: $efectivoCodDeposito")
                                
                                val recoleccionObj = JSONObject()
                                recoleccionObj.put("nombreUbi", ubicacion)
                                recoleccionObj.put("cantidad", cantidad)
                                recoleccionObj.put("codArticulo", codArticulo)
                                recoleccionObj.put("codDeposito", efectivoCodDeposito)
                                recoleccionObj.put("idEtiqueta", codArticulo)
                                recoleccionObj.put("numPartida", partida)
                                //recoleccionObj.put("numSerie", ubicacion)
                                recoleccionObj.put("userData", usuario)
                                
                                recoleccionesArray.put(recoleccionObj)
                            }
                            
                            json.put("recolecciones", recoleccionesArray)
                            json.put("codDeposito", efectivoCodDeposito)
                            
                            // Fecha actual en formato ISO
                            val fechaActual = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date())
                            json.put("fechaHora", fechaActual)
                            
                            json.put("observacion", usuario)
                            json.put("userData", usuario)
                            
                            Log.d("RecolectarScreen", "=== JSON FINAL ===")
                            Log.d("RecolectarScreen", json.toString(2))
                            
                            val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
                            
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    Log.d("RecolectarScreen", "Enviando request a ApiClient.apiService.recolectarPedido...")
                                    
                                    val response = ApiClient.apiService.recolectarPedido(body).execute()
                                    
                                    Log.d("RecolectarScreen", "Respuesta recibida:")
                                    Log.d("RecolectarScreen", "  - Código: ${response.code()}")
                                    Log.d("RecolectarScreen", "  - Exitoso: ${response.isSuccessful}")
                                    
                                    if (response.isSuccessful) {
                                        val responseBody = response.body()?.string() ?: "Sin contenido"
                                        Log.d("RecolectarScreen", "  - Respuesta exitosa: $responseBody")
                                        
                                        withContext(Dispatchers.Main) {
                                            isLoadingEnvio = false
                                            // Limpiar datos después del envío exitoso
                                            scaneoIndividual = mapOf()
                                            cantidadesPorArticulo = mapOf()
                                            cantidadesGuardadas = mapOf()
                                            errorEnvio = null
                                            mensajeExito = null
                                            showSuccessScreen = true
                                            Log.d("RecolectarScreen", "✅ Recolección completada exitosamente")
                                        }
                                    } else {
                                        val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                                        AppLogger.logError(
                                            tag = "RecolectarScreen",
                                            message = "Error en respuesta: code=${response.code()} body=$errorBody"
                                        )
                                        
                                        withContext(Dispatchers.Main) {
                                            isLoadingEnvio = false
                                            errorEnvio = "No se pudo enviar la recolección. Intenta nuevamente."
                                        }
                                    }
                                } catch (e: Exception) {
                                    AppLogger.logError(
                                        tag = "RecolectarScreen",
                                        message = "Excepción durante el envío: ${e.message}",
                                        throwable = e
                                    )
                                    
                                    withContext(Dispatchers.Main) {
                                        isLoadingEnvio = false
                                        errorEnvio = "No se pudo enviar la recolección por un problema de conexión."
                                    }
                                }
                            }
                        }
                    },
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
                Log.d("RecolectarScreen", "Tiene deposito: $tieneDeposito")
                Log.d("RecolectarScreen", "Datos listos (no cargando): $datosListos")
            } else {
                Log.d("RecolectarScreen", "Estado del botón enviar: DESHABILITADO")
                Log.d("RecolectarScreen", "Todas las ubicaciones completas: $todasCompletas")
                Log.d("RecolectarScreen", "Tiene pedido: $tienePedido")
                Log.d("RecolectarScreen", "Tiene deposito: $tieneDeposito")
                Log.d("RecolectarScreen", "Datos listos (no cargando): $datosListos")
                
                // Debug individual de cada artículo
                ubicacionesParsed.forEach { (codArticulo, _) ->
                    val datosEscaneo = scaneoIndividual[codArticulo]
                    val partida = datosEscaneo?.get("partida")
                    val ubicacion = datosEscaneo?.get("ubicacion")
                    val cantidad = cantidadesPorArticulo[codArticulo]
                    val cantidadGuardada = cantidadesGuardadas[codArticulo] == true
                    
                    Log.d("RecolectarScreen", "Debug artículo $codArticulo:")
                    Log.d("RecolectarScreen", "  - Partida: ${partida?.isNotEmpty()}")
                    Log.d("RecolectarScreen", "  - Ubicación: ${ubicacion?.isNotEmpty()}")
                    Log.d("RecolectarScreen", "  - Cantidad: ${cantidad?.isNotEmpty()}")
                    Log.d("RecolectarScreen", "  - Cantidad guardada: $cantidadGuardada")
                }
            }

            // Mostrar error de envío si existe
            if (errorEnvio != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ErrorMessage(
                    message = errorEnvio!!,
                    modifier = Modifier.fillMaxWidth(0.8f)
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
    }
    
    // Mostrar pantalla de éxito cuando se complete la recolección
    if (showSuccessScreen) {
        RecolectarSuccessScreen(
            onFinish = onSuccess
        )
    }
}


@Composable
fun RecolectarScreenPreview() {
    BarCodeScannerTheme {
        RecolectarScreen(
            onBack = {},
            onClose = {},
            onStockearClick = {},
            partida = "123456789",
            ubicacion = "A-01"
        )
    }
}

@Preview(showBackground = true, name = "RecolectarScreen - Error")
@Composable
fun PreviewRecolectarScreenError() {
    var errorUbicaciones by remember { mutableStateOf("No se pudo cargar ubicaciones") }
    RecolectarScreen(
        errorUbicaciones = errorUbicaciones,
        isLoadingUbicaciones = false,
        ubicacionesRecolectar = null,
        qrData = null,
        onRetryUbicaciones = {},
        onStockearClick = {}
    )
}

@Preview(showBackground = true, name = "RecolectarScreen - Éxito")
@Composable
fun PreviewRecolectarScreenSuccess() {
    var mensajeExito by remember { mutableStateOf("¡Recolección exitosa!") }
    RecolectarScreen(
        errorUbicaciones = null,
        isLoadingUbicaciones = false,
        ubicacionesRecolectar = null,
        qrData = null,
        onRetryUbicaciones = {},
        onStockearClick = {}
    )
}

@Preview(showBackground = true, name = "RecolectarScreen - Cargando")
@Composable
fun PreviewRecolectarScreenLoading() {
    RecolectarScreen(
        errorUbicaciones = null,
        isLoadingUbicaciones = true,
        ubicacionesRecolectar = null,
        qrData = null,
        onRetryUbicaciones = {},
        onStockearClick = {}
    )
}

@Preview(showBackground = true, name = "RecolectarScreen - Ubicaciones")
@Composable
fun PreviewRecolectarScreenUbicaciones() {
    val ubicacionesJson = """
        [
            {"numero":"001","nombre":"A-01","codArticulo":"ART123"},
            {"numero":"002","nombre":"B-02","codArticulo":"ART123"},
            {"numero":"003","nombre":"C-03","codArticulo":"ART456"}
        ]
    """.trimIndent()
    RecolectarScreen(
        errorUbicaciones = null,
        isLoadingUbicaciones = false,
        ubicacionesRecolectar = ubicacionesJson,
        qrData = null,
        onRetryUbicaciones = {},
        onStockearClick = {}
    )
}

