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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
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
    producto: String? = null,
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
    onSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    var recolectarItems by remember { mutableStateOf(listOf<RecolectarItem>()) }
    var scanStep by remember { mutableStateOf("producto") }
    var productoActual by remember { mutableStateOf<String?>(null) }
    var ubicacionActual by remember { mutableStateOf<String?>(null) }
    var cantidadActual by remember { mutableStateOf("") }

    var deposito by remember { 
        mutableStateOf(if (fromQR && qrData != null) qrData.deposito else "") 
    }
    var errorEnvio by remember { mutableStateOf<String?>(null) }
    var mensajeExito by remember { mutableStateOf<String?>(null) }
    var showSuccessScreen by remember { mutableStateOf(false) }
    
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

    // Manejo de escaneo
    LaunchedEffect(producto) {
        if (!producto.isNullOrBlank() && scanStep == "producto") {
            productoActual = producto
            scanStep = "ubicacion"
        }
        // Manejo de escaneo individual
        if (!producto.isNullOrBlank() && articuloActualEscaneando != null && tipoEscaneoActual == "producto") {
            val articuloId = articuloActualEscaneando!!
            val datosActuales = scaneoIndividual[articuloId] ?: mapOf()
            scaneoIndividual = scaneoIndividual + (articuloId to (datosActuales + ("producto" to producto)))
            tipoEscaneoActual = null
            articuloActualEscaneando = null
        }
    }
    LaunchedEffect(ubicacion) {
        if (!ubicacion.isNullOrBlank() && scanStep == "ubicacion") {
            ubicacionActual = ubicacion
            scanStep = "cantidad"
        }
        // Manejo de escaneo individual de ubicación
        if (!ubicacion.isNullOrBlank() && articuloActualEscaneando != null && tipoEscaneoActual == "ubicacion") {
            val articuloId = articuloActualEscaneando!!
            val datosActuales = scaneoIndividual[articuloId] ?: mapOf()
            scaneoIndividual = scaneoIndividual + (articuloId to (datosActuales + ("ubicacion" to ubicacion)))
            tipoEscaneoActual = null
            articuloActualEscaneando = null
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
            .background(Color.White)
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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color(0xFF1976D2))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Recolectar",
                        fontSize = 24.sp,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showCloseDialog = true }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color(0xFF1976D2))
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
                            containerColor = Color(0xFF1976D2)
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
                            ubicaciones.add(mapOf(
                                "numero" to ubicacion.optString("numero", "N/A"),
                                "nombre" to ubicacion.optString("nombre", "N/A"),
                               
                                "codArticulo" to ubicacion.optString("codArticulo", "N/A")
                            ))
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
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(Color(0xFFF3E5F5), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text("Ubicaciones para Recolectar:", color = Color(0xFF7B1FA2), fontSize = 18.sp)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    when (ubicacionesParsed.first) {
                        "array" -> {
                            Text("Ubicaciones:", color = Color(0xFF7B1FA2), fontSize = 14.sp)
                            val ubicaciones = ubicacionesParsed.second as List<Map<String, Any>>
                            
                            // Agrupar por codArticulo
                            val ubicacionesGrouped = ubicaciones.groupBy { it["codArticulo"] as String }
                            
                            // Estado para dropdowns expandidos
                            var expandedItems by remember { mutableStateOf(setOf<String>()) }
                            
                            ubicacionesGrouped.forEach { (codArticulo, ubicacionesDelArticulo) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF8F8F8)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        // Título del artículo
                                        Text(
                                            text = "Código Artículo: $codArticulo",
                                            color = Color(0xFF1976D2),
                                            fontSize = 16.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Estado del escaneo para este artículo
                                        val datosEscaneo = scaneoIndividual[codArticulo] ?: mapOf()
                                        val productoEscaneado = datosEscaneo["producto"]
                                        val ubicacionEscaneada = datosEscaneo["ubicacion"]
                                        val cantidadArticulo = cantidadesPorArticulo[codArticulo] ?: ""
                                        
                                        // Estado de campos editables para este artículo
                                        val camposArticulo = camposEditables[codArticulo] ?: mapOf("producto" to false, "ubicacion" to false)
                                        val productoEditable = camposArticulo["producto"] ?: false
                                        val ubicacionEditable = camposArticulo["ubicacion"] ?: false
                                        
                                        // Estado de cantidad guardada para este artículo
                                        val cantidadGuardada = cantidadesGuardadas[codArticulo] ?: false
                                        
                                        // Campos de entrada siempre visibles
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Campo producto (siempre visible)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = productoEscaneado ?: "",
                                                onValueChange = { newValue ->
                                                    if (productoEditable) {
                                                        val articuloId = codArticulo
                                                        val datosActuales = scaneoIndividual[articuloId] ?: mapOf()
                                                        scaneoIndividual = scaneoIndividual + (articuloId to (datosActuales + ("producto" to newValue)))
                                                    }
                                                },
                                                label = { Text("Producto", color = Color.Black) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                                    cursorColor = Color.Black,
                                                    focusedBorderColor = Color(0xFF1976D2),
                                                    unfocusedBorderColor = Color(0xFF1976D2),
                                                    focusedLabelColor = Color.Black,
                                                    unfocusedLabelColor = Color.Black
                                                ),
                                                placeholder = { Text("Código del producto", color = Color.Gray) },
                                                enabled = productoEditable,
                                                readOnly = !productoEditable
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = {
                                                    val articuloId = codArticulo
                                                    val camposActuales = camposEditables[articuloId] ?: mapOf("producto" to false, "ubicacion" to false)
                                                    camposEditables = camposEditables + (articuloId to (camposActuales + ("producto" to !productoEditable)))
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Filled.Edit,
                                                    contentDescription = if (productoEditable) "Deshabilitar edición" else "Habilitar edición",
                                                    tint = if (productoEditable) Color(0xFF4CAF50) else Color(0xFF1976D2),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Campo ubicación (siempre visible)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = ubicacionEscaneada ?: "",
                                                onValueChange = { newValue ->
                                                    if (ubicacionEditable) {
                                                        val articuloId = codArticulo
                                                        val datosActuales = scaneoIndividual[articuloId] ?: mapOf()
                                                        scaneoIndividual = scaneoIndividual + (articuloId to (datosActuales + ("ubicacion" to newValue)))
                                                    }
                                                },
                                                label = { Text("Ubicación", color = Color.Black) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                                    cursorColor = Color.Black,
                                                    focusedBorderColor = Color(0xFF1976D2),
                                                    unfocusedBorderColor = Color(0xFF1976D2),
                                                    focusedLabelColor = Color.Black,
                                                    unfocusedLabelColor = Color.Black
                                                ),
                                                placeholder = { Text("Código de ubicación", color = Color.Gray) },
                                                enabled = ubicacionEditable,
                                                readOnly = !ubicacionEditable
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = {
                                                    val articuloId = codArticulo
                                                    val camposActuales = camposEditables[articuloId] ?: mapOf("producto" to false, "ubicacion" to false)
                                                    camposEditables = camposEditables + (articuloId to (camposActuales + ("ubicacion" to !ubicacionEditable)))
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Filled.Edit,
                                                    contentDescription = if (ubicacionEditable) "Deshabilitar edición" else "Habilitar edición",
                                                    tint = if (ubicacionEditable) Color(0xFF4CAF50) else Color(0xFF1976D2),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        
                                        // Campo cantidad (visible cuando producto y ubicación están completos)
                                        if ((productoEscaneado?.isNotEmpty() == true) && (ubicacionEscaneada?.isNotEmpty() == true)) {
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
                                                            focusedBorderColor = Color(0xFF1976D2),
                                                            unfocusedBorderColor = Color(0xFF1976D2),
                                                            focusedLabelColor = Color.Black,
                                                            unfocusedLabelColor = Color.Black
                                                        ),
                                                        placeholder = { Text("Ingrese la cantidad", color = Color.Gray) }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Button(
                                                        onClick = {
                                                            if (cantidadArticulo.isNotEmpty()) {
                                                                cantidadesGuardadas = cantidadesGuardadas + (codArticulo to true)
                                                            }
                                                        },
                                                        enabled = cantidadArticulo.isNotEmpty(),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF4CAF50)
                                                        ),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Check,
                                                            contentDescription = "Guardar cantidad",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
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
                                                        label = { Text("Cantidad", color = Color.Black) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                                            cursorColor = Color.Black,
                                                            focusedBorderColor = Color(0xFF1976D2),
                                                            unfocusedBorderColor = Color(0xFF1976D2),
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
                                                            tint = Color(0xFF1976D2),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        
                                        if (ubicacionesDelArticulo.size == 1) {
                                            // Solo una ubicación - mostrar directamente
                                            val ubicacion = ubicacionesDelArticulo[0]
                                            Column {
                                                Text(
                                                    text = "Ubicación: ${ubicacion["nombre"]}",
                                                    color = Color.Black,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Número: ${ubicacion["numero"]}",
                                                    color = Color.Gray,
                                                    fontSize = 12.sp
                                                )
                                                if (ubicacion["alias"] != "N/A") {
                                                    Text(
                                                        text = "Alias: ${ubicacion["alias"]}",
                                                        color = Color.Gray,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                Text(
                                                    text = "Orden: ${ubicacion["orden"]}",
                                                    color = Color(0xFF7B1FA2),
                                                    fontSize = 12.sp
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
                                                    .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Ubicaciones (${ubicacionesDelArticulo.size})",
                                                    color = Color(0xFF1976D2),
                                                    fontSize = 14.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                                )
                                                Text(
                                                    text = if (isExpanded) "▼" else "▶",
                                                    color = Color(0xFF1976D2),
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
                                                                    text = "${ubicacion["nombre"]}",
                                                                    color = Color.Black,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                                                )
                                                                Text(
                                                                    text = "Número: ${ubicacion["numero"]}",
                                                                    color = Color(0xFF2E7D32),
                                                                    fontSize = 12.sp,
                                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                                )
                                                            }
                                                            if (ubicacion["alias"] != "N/A") {
                                                                Text(
                                                                    text = "Alias: ${ubicacion["alias"]}",
                                                                    color = Color.Gray,
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                            Text(
                                                                text = "Orden: ${ubicacion["orden"]}",
                                                                color = Color(0xFF7B1FA2),
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Botón de escaneo dinámico
                                        if (productoEscaneado?.isEmpty() != false) {
                                            // Mostrar botón escanear producto
                                            Button(
                                                onClick = {
                                                    articuloActualEscaneando = codArticulo
                                                    tipoEscaneoActual = "producto"
                                                    onStockearClick("producto")
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF1976D2)
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.QrCodeScanner,
                                                        contentDescription = "Escanear producto",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Escanear Producto",
                                                        color = Color.White,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        } else if (ubicacionEscaneada?.isEmpty() != false) {
                                            // Mostrar botón escanear ubicación
                                            Button(
                                                onClick = {
                                                    articuloActualEscaneando = codArticulo
                                                    tipoEscaneoActual = "ubicacion"
                                                    onStockearClick("ubicacion")
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF1976D2)
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
                                                        modifier = Modifier.size(16.dp)
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
                                            // Mostrar botones de reescaneo
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        articuloActualEscaneando = codArticulo
                                                        tipoEscaneoActual = "producto"
                                                        onStockearClick("producto")
                                                    },
                                                    modifier = Modifier.weight(1f),
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
                                                            imageVector = Icons.Filled.Refresh,
                                                            contentDescription = "Re-escanear producto",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Re-Producto",
                                                            color = Color.White,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                                Button(
                                                    onClick = {
                                                        articuloActualEscaneando = codArticulo
                                                        tipoEscaneoActual = "ubicacion"
                                                        onStockearClick("ubicacion")
                                                    },
                                                    modifier = Modifier.weight(1f),
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
                                                            imageVector = Icons.Filled.Refresh,
                                                            contentDescription = "Re-escanear ubicación",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Re-Ubicación",
                                                            color = Color.White,
                                                            fontSize = 10.sp
                                                        )
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
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Botón enviar - validación basada en items escaneados y número de pedido
            val ubicacionesParsed = remember(ubicacionesRecolectar) {
                try {
                    val jsonArray = JSONArray(ubicacionesRecolectar ?: "[]")
                    val ubicaciones = mutableListOf<Map<String, Any>>()
                    for (i in 0 until jsonArray.length()) {
                        val ubicacion = jsonArray.getJSONObject(i)
                        ubicaciones.add(mapOf(
                            "numero" to ubicacion.optString("numero", "N/A"),
                            "nombre" to ubicacion.optString("nombre", "N/A"),
                            "alias" to ubicacion.optString("alias", "N/A"),
                            "orden" to ubicacion.optInt("orden", 0),
                            "codArticulo" to ubicacion.optString("codArticulo", "N/A")
                        ))
                    }
                    ubicaciones.groupBy { it["codArticulo"] as String }
                } catch (e: Exception) {
                    mapOf<String, List<Map<String, Any>>>()
                }
            }

            // Verificar si todos los items escaneados están completos y hay número de pedido
            val todasCompletas = ubicacionesParsed.all { (codArticulo, _) ->
                val datosEscaneo = scaneoIndividual[codArticulo]
                val producto = datosEscaneo?.get("producto")
                val ubicacion = datosEscaneo?.get("ubicacion")
                val cantidad = cantidadesPorArticulo[codArticulo]
                val cantidadGuardada = cantidadesGuardadas[codArticulo] == true
                
                !producto.isNullOrEmpty() && !ubicacion.isNullOrEmpty() && 
                !cantidad.isNullOrEmpty() && cantidadGuardada
            }

            val tienePedido = qrData?.pedido?.isNotEmpty() == true
            val tieneDeposito = (qrData?.deposito?.isNotEmpty() == true) || deposito.isNotBlank()
            val datosListos = !isLoadingUbicaciones && errorUbicaciones == null && ubicacionesRecolectar != null

            if (todasCompletas && tienePedido && tieneDeposito && datosListos) {
                Button(
                    onClick = {
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
                            val producto = datosEscaneo["producto"] ?: ""
                            val ubicacion = datosEscaneo["ubicacion"] ?: ""
                            val cantidad = cantidadesPorArticulo[codArticulo]?.toIntOrNull() ?: 0
                            
                            Log.d("RecolectarScreen", "Artículo: $codArticulo")
                            Log.d("RecolectarScreen", "  - Producto: $producto")
                            Log.d("RecolectarScreen", "  - Ubicación: $ubicacion")
                            Log.d("RecolectarScreen", "  - Cantidad: $cantidad")
                            Log.d("RecolectarScreen", "  - CodDeposito: $efectivoCodDeposito")
                            
                            val recoleccionObj = JSONObject()
                            recoleccionObj.put("nombreUbi", ubicacion)
                            recoleccionObj.put("cantidad", cantidad)
                            recoleccionObj.put("codArticulo", codArticulo)
                            recoleccionObj.put("codDeposito", efectivoCodDeposito)
                            recoleccionObj.put("idEtiqueta", ubicacion)
                            recoleccionObj.put("numPartida", producto)
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
                                    Log.e("RecolectarScreen", "❌ Error en respuesta: $errorBody")
                                    
                                    withContext(Dispatchers.Main) {
                                        errorEnvio = "Error ${response.code()}: $errorBody"
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("RecolectarScreen", "❌ Excepción durante el envío: ${e.message}", e)
                                
                                withContext(Dispatchers.Main) {
                                    errorEnvio = "Error de conexión: ${e.message ?: "Error desconocido"}"
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Enviar recolección",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar Recolección", color = Color.White, fontSize = 16.sp)
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
                    val producto = datosEscaneo?.get("producto")
                    val ubicacion = datosEscaneo?.get("ubicacion")
                    val cantidad = cantidadesPorArticulo[codArticulo]
                    val cantidadGuardada = cantidadesGuardadas[codArticulo] == true
                    
                    Log.d("RecolectarScreen", "Debug artículo $codArticulo:")
                    Log.d("RecolectarScreen", "  - Producto: ${producto?.isNotEmpty()}")
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
                onDismissRequest = { showBackDialog = false },
                title = {
                    Text(
                        text = "Confirmar regreso",
                        color = Color(0xFF1976D2),
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
                        Text("Sí, regresar", color = Color(0xFF1976D2))
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
                onDismissRequest = { showCloseDialog = false },
                title = {
                    Text(
                        text = "Confirmar salida",
                        color = Color(0xFF1976D2),
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
                        Text("Sí, ir al menú", color = Color(0xFF1976D2))
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
            producto = "123456789",
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

