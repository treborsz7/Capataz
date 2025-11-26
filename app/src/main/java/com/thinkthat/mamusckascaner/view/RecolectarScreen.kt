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
    
    // Estado para escaneo individual por codArticulo - AHORA soporta múltiples escaneos por artículo
    // Estructura: Map<codArticulo, List<Map<String, String>>>
    // Cada elemento de la lista representa un escaneo (partida, ubicacion, cantidad)
    var scaneoIndividual by remember { mutableStateOf(mapOf<String, List<Map<String, String>>>()) }
    var articuloActualEscaneando by remember { mutableStateOf<String?>(null) }
    var tipoEscaneoActual by remember { mutableStateOf<String?>(null) }
    var indiceEscaneoActual by remember { mutableStateOf<Int?>(null) } // Para saber qué escaneo estamos editando
    
    // Estado para cantidades por artículo - AHORA por índice de escaneo
    var cantidadesPorArticulo by remember { mutableStateOf(mapOf<String, Map<Int, String>>()) }
    
    // Estado para campos editables por artículo - AHORA por índice de escaneo
    var camposEditables by remember { mutableStateOf(mapOf<String, Map<Int, Map<String, Boolean>>>()) }
    
    // Estado para cantidades guardadas por artículo - AHORA por índice de escaneo
    var cantidadesGuardadas by remember { mutableStateOf(mapOf<String, Map<Int, Boolean>>()) }

    // Manejo de escaneo (individual por ítem con soporte múltiple). 
    // Cada botón establece articuloActualEscaneando, tipoEscaneoActual e indiceEscaneoActual
    LaunchedEffect(partida, articuloActualEscaneando, tipoEscaneoActual, indiceEscaneoActual) {
        if (partida.isNullOrBlank()) return@LaunchedEffect

        // Flujo legacy secuencial (solo si se usa aún scanStep global). No afecta la lógica individual.
        if (scanStep == "partida" && articuloActualEscaneando == null && tipoEscaneoActual == null) {
            partidaActual = partida
            scanStep = "ubicacion"
        }

        if (articuloActualEscaneando != null && tipoEscaneoActual == "partida" && indiceEscaneoActual != null) {
            val articuloId = articuloActualEscaneando!!
            val indice = indiceEscaneoActual!!
            
            val listaEscaneos = scaneoIndividual[articuloId] ?: emptyList()
            val escaneoActual = listaEscaneos.getOrNull(indice) ?: emptyMap()
            val nuevoEscaneo = escaneoActual + ("partida" to partida)
            
            val nuevaLista = listaEscaneos.toMutableList()
            if (indice < nuevaLista.size) {
                nuevaLista[indice] = nuevoEscaneo
            } else {
                nuevaLista.add(nuevoEscaneo)
            }
            
            scaneoIndividual = scaneoIndividual + (articuloId to nuevaLista)

            // Forzamos el campo a editable para visualizar inmediato el valor escaneado
            val camposArticulo = camposEditables[articuloId] ?: emptyMap()
            val camposIndice = camposArticulo[indice] ?: mapOf("partida" to false, "ubicacion" to false)
            val nuevosCamposIndice = camposIndice + ("partida" to true)
            camposEditables = camposEditables + (articuloId to (camposArticulo + (indice to nuevosCamposIndice)))

            // Limpiar banderas de escaneo para permitir próximos escaneos
            tipoEscaneoActual = null
            articuloActualEscaneando = null
            indiceEscaneoActual = null
            // Limpiar valor escaneado de la memoria
            onClearScanValues()
        } else {
            Log.d("RecolectarScreen", "[SCAN-IGNORED] Partida recibido pero sin target válido (articuloActualEscaneando=$articuloActualEscaneando tipoEscaneoActual=$tipoEscaneoActual indice=$indiceEscaneoActual)")
        }
    }

    LaunchedEffect(ubicacion, articuloActualEscaneando, tipoEscaneoActual, indiceEscaneoActual) {
        if (ubicacion.isNullOrBlank()) return@LaunchedEffect

        // Flujo legacy secuencial global
        if (scanStep == "ubicacion" && articuloActualEscaneando == null && tipoEscaneoActual == null) {
            ubicacionActual = ubicacion
            scanStep = "cantidad"
        }

        if (articuloActualEscaneando != null && tipoEscaneoActual == "ubicacion" && indiceEscaneoActual != null) {
            val articuloId = articuloActualEscaneando!!
            val indice = indiceEscaneoActual!!
            
            val listaEscaneos = scaneoIndividual[articuloId] ?: emptyList()
            val escaneoActual = listaEscaneos.getOrNull(indice) ?: emptyMap()
            val nuevoEscaneo = escaneoActual + ("ubicacion" to ubicacion)
            
            val nuevaLista = listaEscaneos.toMutableList()
            if (indice < nuevaLista.size) {
                nuevaLista[indice] = nuevoEscaneo
            } else {
                nuevaLista.add(nuevoEscaneo)
            }
            
            scaneoIndividual = scaneoIndividual + (articuloId to nuevaLista)

            val camposArticulo = camposEditables[articuloId] ?: emptyMap()
            val camposIndice = camposArticulo[indice] ?: mapOf("partida" to false, "ubicacion" to false)
            val nuevosCamposIndice = camposIndice + ("ubicacion" to true)
            camposEditables = camposEditables + (articuloId to (camposArticulo + (indice to nuevosCamposIndice)))

            Log.d("RecolectarScreen", "[SCAN] Ubicacion asignada -> articulo=$articuloId indice=$indice valor=$ubicacion")
            tipoEscaneoActual = null
            articuloActualEscaneando = null
            indiceEscaneoActual = null
            // Limpiar valor escaneado de la memoria
            onClearScanValues()
        } else {
            Log.d("RecolectarScreen", "[SCAN-IGNORED] Ubicación recibida pero sin target válido (articuloActualEscaneando=$articuloActualEscaneando tipoEscaneoActual=$tipoEscaneoActual indice=$indiceEscaneoActual)")
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
                                // Inicializar el primer escaneo si no existe
                                LaunchedEffect(codArticulo) {
                                    val listaActual = scaneoIndividual[codArticulo]
                                    if (listaActual == null || listaActual.isEmpty()) {
                                        scaneoIndividual = scaneoIndividual + (codArticulo to listOf(emptyMap()))
                                    }
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
                                        
                                        // Mostrar cantidad solicitada y recolectada
                                        val cantidadSolicitada = ubicacionesDelArticulo.firstOrNull()?.get("requerido") as? Int ?: 0
                                        
                                        // Calcular total recolectado de todos los escaneos
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
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Solicitado: $cantidadSolicitada",
                                                color = Color.Black,
                                                fontSize = 14.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                                            )
                                            Text(
                                                text = "Recolectado: $totalRecolectado",
                                                color = if (totalRecolectado >= cantidadSolicitada) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                                fontSize = 14.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Renderizar cada escaneo guardado (con cantidad confirmada)
                                        listaEscaneos.forEachIndexed { indice, escaneo ->
                                            val partidaEscaneado = escaneo["partida"]
                                            val ubicacionEscaneada = escaneo["ubicacion"]
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
                                                    val cantidadesMap = cantidadesArticulo + (indice to cantidadAutoLlenar.toString())
                                                    cantidadesPorArticulo = cantidadesPorArticulo + (codArticulo to cantidadesMap)
                                                }
                                            }
                                            
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (cantidadGuardada) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
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
                                                        // Mostrar ubicación asignada en lugar de "Escaneo #X"
                                                        if (indice < ubicacionesDelArticulo.size) {
                                                            val ubicacionAsignada = ubicacionesDelArticulo[indice]
                                                            Text(
                                                                text = (ubicacionAsignada["nombreUbicacion"] as? String) ?: "N/A",
                                                                fontSize = 14.sp,
                                                                color = Color(0xFF4CAF50),
                                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                            )
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
                                                                    // Eliminar este escaneo de la lista
                                                                    val listaActual = scaneoIndividual[codArticulo] ?: emptyList()
                                                                    val nuevaLista = listaActual.toMutableList()
                                                                    if (indice < nuevaLista.size) {
                                                                        nuevaLista.removeAt(indice)
                                                                    }
                                                                    scaneoIndividual = scaneoIndividual + (codArticulo to nuevaLista)
                                                                    
                                                                    // También eliminar la cantidad guardada
                                                                    val cantidadesArt = cantidadesPorArticulo[codArticulo]?.toMutableMap() ?: mutableMapOf()
                                                                    cantidadesArt.remove(indice)
                                                                    // Reindexar las cantidades restantes
                                                                    val nuevasCantidades = mutableMapOf<Int, String>()
                                                                    cantidadesArt.entries.sortedBy { it.key }.forEachIndexed { newIndex, entry ->
                                                                        if (entry.key > indice) {
                                                                            nuevasCantidades[newIndex] = entry.value
                                                                        } else if (entry.key < indice) {
                                                                            nuevasCantidades[entry.key] = entry.value
                                                                        }
                                                                    }
                                                                    cantidadesPorArticulo = cantidadesPorArticulo + (codArticulo to nuevasCantidades)
                                                                    
                                                                    // Eliminar estado de guardado
                                                                    val guardadasArt = cantidadesGuardadas[codArticulo]?.toMutableMap() ?: mutableMapOf()
                                                                    guardadasArt.remove(indice)
                                                                    // Reindexar estados guardados
                                                                    val nuevasGuardadas = mutableMapOf<Int, Boolean>()
                                                                    guardadasArt.entries.sortedBy { it.key }.forEachIndexed { newIndex, entry ->
                                                                        if (entry.key > indice) {
                                                                            nuevasGuardadas[newIndex] = entry.value
                                                                        } else if (entry.key < indice) {
                                                                            nuevasGuardadas[entry.key] = entry.value
                                                                        }
                                                                    }
                                                                    cantidadesGuardadas = cantidadesGuardadas + (codArticulo to nuevasGuardadas)
                                                                    
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
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                OutlinedTextField(
                                                                    value = partidaEscaneado ?: "",
                                                                    onValueChange = { newValue ->
                                                                        val listaActual = scaneoIndividual[codArticulo] ?: emptyList()
                                                                        val escaneoActual = listaActual.getOrNull(indice) ?: emptyMap()
                                                                        val nuevoEscaneo = escaneoActual + ("partida" to newValue)
                                                                        val nuevaLista = listaActual.toMutableList()
                                                                        if (indice < nuevaLista.size) {
                                                                            nuevaLista[indice] = nuevoEscaneo
                                                                        }
                                                                        scaneoIndividual = scaneoIndividual + (codArticulo to nuevaLista)
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
                                                                articuloActualEscaneando = codArticulo
                                                                tipoEscaneoActual = "partida"
                                                                indiceEscaneoActual = indice
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
                                                    } else {
                                                        // Partida ya escaneada
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
                                                                        color = Color.Black,
                                                                        fontSize = 16.sp
                                                                    )
                                                                }
                                                                IconButton(
                                                                    onClick = {
                                                                        articuloActualEscaneando = codArticulo
                                                                        tipoEscaneoActual = "partida"
                                                                        indiceEscaneoActual = indice
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
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                OutlinedTextField(
                                                                    value = partidaEscaneado,
                                                                    onValueChange = { newValue ->
                                                                        val listaActual = scaneoIndividual[codArticulo] ?: emptyList()
                                                                        val escaneoActual = listaActual.getOrNull(indice) ?: emptyMap()
                                                                        val nuevoEscaneo = escaneoActual + ("partida" to newValue)
                                                                        val nuevaLista = listaActual.toMutableList()
                                                                        if (indice < nuevaLista.size) {
                                                                            nuevaLista[indice] = nuevoEscaneo
                                                                        }
                                                                        scaneoIndividual = scaneoIndividual + (codArticulo to nuevaLista)
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
                                                                        val camposArt = camposEditables[codArticulo] ?: emptyMap()
                                                                        val camposInd = camposArt[indice] ?: mapOf("partida" to false, "ubicacion" to false)
                                                                        camposEditables = camposEditables + (codArticulo to (camposArt + (indice to (camposInd + ("partida" to false)))))
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
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    OutlinedTextField(
                                                                        value = ubicacionEscaneada ?: "",
                                                                        onValueChange = { newValue ->
                                                                            val listaActual = scaneoIndividual[codArticulo] ?: emptyList()
                                                                            val escaneoActual = listaActual.getOrNull(indice) ?: emptyMap()
                                                                            val nuevoEscaneo = escaneoActual + ("ubicacion" to newValue)
                                                                            val nuevaLista = listaActual.toMutableList()
                                                                            if (indice < nuevaLista.size) {
                                                                                nuevaLista[indice] = nuevoEscaneo
                                                                            }
                                                                            scaneoIndividual = scaneoIndividual + (codArticulo to nuevaLista)
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
                                                                    articuloActualEscaneando = codArticulo
                                                                    tipoEscaneoActual = "ubicacion"
                                                                    indiceEscaneoActual = indice
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
                                                            // Ubicación ya escaneada
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
                                                                            color = Color.Black,
                                                                            fontSize = 16.sp
                                                                        )
                                                                    }
                                                                    IconButton(
                                                                        onClick = {
                                                                            articuloActualEscaneando = codArticulo
                                                                            tipoEscaneoActual = "ubicacion"
                                                                            indiceEscaneoActual = indice
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
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    OutlinedTextField(
                                                                        value = ubicacionEscaneada,
                                                                        onValueChange = { newValue ->
                                                                            val listaActual = scaneoIndividual[codArticulo] ?: emptyList()
                                                                            val escaneoActual = listaActual.getOrNull(indice) ?: emptyMap()
                                                                            val nuevoEscaneo = escaneoActual + ("ubicacion" to newValue)
                                                                            val nuevaLista = listaActual.toMutableList()
                                                                            if (indice < nuevaLista.size) {
                                                                                nuevaLista[indice] = nuevoEscaneo
                                                                            }
                                                                            scaneoIndividual = scaneoIndividual + (codArticulo to nuevaLista)
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
                                                                            val camposArt = camposEditables[codArticulo] ?: emptyMap()
                                                                            val camposInd = camposArt[indice] ?: mapOf("partida" to false, "ubicacion" to false)
                                                                            camposEditables = camposEditables + (codArticulo to (camposArt + (indice to (camposInd + ("ubicacion" to false)))))
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
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    OutlinedTextField(
                                                                        value = cantidadEscaneo,
                                                                        onValueChange = { newValue ->
                                                                            val filteredValue = newValue.filter { it.isDigit() }
                                                                            val cantidadesMap = cantidadesArticulo + (indice to filteredValue)
                                                                            cantidadesPorArticulo = cantidadesPorArticulo + (codArticulo to cantidadesMap)
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
                                                                            if (cantidadEscaneo.isNotEmpty()) {
                                                                                val cantidadesGuardMap = cantidadesGuardadasArticulo + (indice to true)
                                                                                cantidadesGuardadas = cantidadesGuardadas + (codArticulo to cantidadesGuardMap)
                                                                            }
                                                                        },
                                                                        enabled = cantidadEscaneo.isNotEmpty()
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Filled.Save,
                                                                            contentDescription = "Guardar cantidad",
                                                                            tint = if (cantidadEscaneo.isNotEmpty()) Color(0xFF4CAF50) else Color.Gray,
                                                                            modifier = Modifier.size(24.dp)
                                                                        )
                                                                    }
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
                                                                            val cantidadesGuardMap = cantidadesGuardadasArticulo + (indice to false)
                                                                            cantidadesGuardadas = cantidadesGuardadas + (codArticulo to cantidadesGuardMap)
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
                                            val partidaLlena = !ultimoEscaneo["partida"].isNullOrEmpty()
                                            val ubicacionLlena = !ultimoEscaneo["ubicacion"].isNullOrEmpty()
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
                                                    // Agregar nuevo escaneo vacío a la lista
                                                    val listaActual = scaneoIndividual[codArticulo] ?: emptyList()
                                                    val nuevaLista = listaActual + mapOf<String, String>()
                                                    scaneoIndividual = scaneoIndividual + (codArticulo to nuevaLista)
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
            // Se permite envío con cargas parciales, solo se valida que los renglones existentes estén completos
            val todasCompletas = ubicacionesParsed.all { (codArticulo, _) ->
                val listaEscaneos = scaneoIndividual[codArticulo] ?: emptyList()
                val cantidadesArticulo = cantidadesPorArticulo[codArticulo] ?: emptyMap()
                val cantidadesGuardadasArticulo = cantidadesGuardadas[codArticulo] ?: emptyMap()
                
                // Verificar que haya al menos un escaneo y que TODOS los escaneos de este artículo estén completos
                // (con partida, ubicación y cantidad guardada)
                val hayEscaneos = listaEscaneos.isNotEmpty()
                val todosLosEscaneosCompletos = listaEscaneos.all { escaneo ->
                    val indice = listaEscaneos.indexOf(escaneo)
                    val guardado = cantidadesGuardadasArticulo[indice] ?: false
                    val partida = escaneo["partida"]
                    val ubicacion = escaneo["ubicacion"]
                    val tieneCantidad = cantidadesArticulo[indice]?.isNotEmpty() == true
                    
                    !partida.isNullOrEmpty() && !ubicacion.isNullOrEmpty() && tieneCantidad && guardado
                }
                
                hayEscaneos && todosLosEscaneosCompletos
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
                            scaneoIndividual.forEach { (codArticulo, listaEscaneos) ->
                                // Iterar sobre cada escaneo del artículo
                                listaEscaneos.forEachIndexed { indice, escaneo ->
                                    val partida = escaneo["partida"] ?: ""
                                    val ubicacion = escaneo["ubicacion"] ?: ""
                                    val cantidadesArticulo = cantidadesPorArticulo[codArticulo] ?: emptyMap()
                                    val cantidadesGuardadasArticulo = cantidadesGuardadas[codArticulo] ?: emptyMap()
                                    val cantidad = cantidadesArticulo[indice]?.toIntOrNull() ?: 0
                                    val cantidadGuardada = cantidadesGuardadasArticulo[indice] ?: false
                                    
                                    // Solo incluir escaneos que tengan cantidad guardada
                                    if (cantidadGuardada && partida.isNotEmpty() && ubicacion.isNotEmpty() && cantidad > 0) {
                                        Log.d("RecolectarScreen", "Artículo: $codArticulo - Escaneo #${indice + 1}")
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
                                }
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

