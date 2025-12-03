import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalConfiguration
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thinkthat.mamusckascaner.service.Services.ApiClient
import com.thinkthat.mamusckascaner.service.Services.EstibarPartida
import com.thinkthat.mamusckascaner.service.Services.EstibarPartidasRequest
import com.thinkthat.mamusckascaner.service.Services.UbicacionResponse
import com.thinkthat.mamusckascaner.utils.AppLogger
import com.thinkthat.mamusckascaner.view.EstivacionSuccessActivity
import com.thinkthat.mamusckascaner.view.components.ErrorMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstivacionScreen(
    onBack: () -> Unit = {},
    onStockearClick: (boton: String) -> Unit = {},
    producto: String? = null,
    ubicacion: String?
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // Responsive values based on screen size
    val horizontalPadding = maxOf(minOf(screenWidth * 0.08f, 32.dp), 16.dp)
    val logoSize = maxOf(minOf(screenWidth * 0.6f, 350.dp), 200.dp)
    val formWidth = 0.85f
    val topOffset = -maxOf(minOf(screenHeight * 0.05f, 40.dp), 20.dp)
    val titleFontSize = maxOf(minOf((screenWidth * 0.06f).value, 28f), 20f).sp
    val bodyFontSize = maxOf(minOf((screenWidth * 0.04f).value, 18f), 14f).sp
    val buttonHeight = maxOf(minOf(screenHeight * 0.07f, 64.dp), 48.dp)
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    // Leer depósito desde SharedPreferences (guardado en login) - solo para uso en API
    val prefs = context.getSharedPreferences("QRCodeScannerPrefs", Context.MODE_PRIVATE)
    val deposito = prefs.getString("savedDeposito", "") ?: ""
    
    // Estado para productos (lista)
    var productos by remember { mutableStateOf(listOf<String>()) }
    var productosEditables by remember { mutableStateOf(mapOf<Int, Boolean>()) }
    var productosFieldValues by remember { mutableStateOf(mapOf<Int, TextFieldValue>()) }
    
    // Estado para partida individual editable
    var partidaLocal by remember { mutableStateOf(producto ?: "") }
    var partidaEditable by remember { mutableStateOf(false) }
    var partidaFieldValue by remember { mutableStateOf(TextFieldValue(producto ?: "")) }
    val partidaFocusRequester = remember { FocusRequester() }
    
    // Estado para ubicación editable - usar variable local mutable
    var ubicacionLocal by remember { mutableStateOf(ubicacion ?: "") }
    var ubicacionEditable by remember { mutableStateOf(false) }
    var ubicacionFieldValue by remember { mutableStateOf(TextFieldValue(ubicacion ?: "")) }
    val ubicacionFocusRequester = remember { FocusRequester() }
    
    // Si viene un producto por parámetro (de la cámara), agregarlo a la lista
    LaunchedEffect(producto) {
        if (!producto.isNullOrBlank()) {
            partidaLocal = producto
            partidaFieldValue = TextFieldValue(producto)
            // Salir del modo editable si estaba activo
            partidaEditable = false
            // Agregar a la lista si no existe ya
            if (!productos.contains(producto)) {
                productos = productos + producto
            }
        }
    }
    
    // Actualizar ubicacionLocal y ubicacionFieldValue cuando cambie ubicacion
    LaunchedEffect(ubicacion) {
        if (ubicacion != null) {
            ubicacionLocal = ubicacion
            ubicacionFieldValue = TextFieldValue(ubicacion)
            // Salir del modo editable si estaba activo
            ubicacionEditable = false
        }
    }
    // Estado para ubicaciones
    var ubicaciones by remember { mutableStateOf(listOf<UbicacionResponse>()) }
    //var ubicacionSeleccionada by remember { mutableStateOf<String?>(null) }
    var expandedUbicaciones by remember { mutableStateOf(false) }
    var ubicacionSeleccionada by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorEnvio by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission())
    { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCD0914))
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        // Botón de volver en la esquina superior izquierda
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontalPadding / 2)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
        }
        
        // Título centrado a la misma altura que el botón de volver
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
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((screenHeight * 0.015f).coerceAtLeast(8.dp).coerceAtMost(16.dp)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .offset(y = topOffset)
                .align(Alignment.Center)
        ) {
            // Logo centrado
            Icon(
                painter = painterResource(id = com.thinkthat.mamusckascaner.R.drawable.logos_y__1__05__1_),
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(logoSize)
            )
            
            // Lógica secuencial: mostrar campos y botones según el estado
            if (partidaLocal.isBlank()) {
                // 1. Solo mostrar campo partida
                // Campo Partida con ícono de cámara y edición
                if (!partidaEditable) {
                    // Modo solo lectura - sin bordes, texto gris
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Transparent)
                                .padding(horizontalPadding / 2)
                        ) {
                            Text(
                                text = "Partida:",
                                color = Color.White,
                                fontSize = bodyFontSize
                            )
                        }
                        // No mostrar ícono de cámara cuando el campo está vacío
                        IconButton(
                            onClick = { 
                                partidaEditable = true
                                partidaFieldValue = TextFieldValue(
                                    text = partidaLocal,
                                    selection = TextRange(0, partidaLocal.length)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar partida",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    // Modo edición - campo normal con ícono de guardar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = partidaFieldValue,
                            onValueChange = { partidaFieldValue = it },
                            label = { Text("Partida", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(partidaFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { 
                                partidaLocal = partidaFieldValue.text
                                partidaEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar partida",
                                tint = Color.White
                            )
                        }
                    }
                    
                    // Auto-focus y selección cuando se habilita la edición
                    LaunchedEffect(partidaEditable) {
                        if (partidaEditable) {
                            partidaFocusRequester.requestFocus()
                        }
                    }
                }
            } else if (ubicacionLocal.isBlank()) {
                // 2. Mostrar campo partida (completado) + campo ubicación
                // Campo Partida (completado)
                if (!partidaEditable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Transparent)
                                .padding(horizontalPadding / 2)
                        ) {
                            Text(
                                text = "Partida: $partidaLocal",
                                color = Color.White,
                                fontSize = bodyFontSize
                            )
                        }
                        IconButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    onStockearClick("producto")
                                } else {
                                    launcher.launch(android.Manifest.permission.CAMERA)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Escanear partida",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { 
                                partidaEditable = true
                                partidaFieldValue = TextFieldValue(
                                    text = partidaLocal,
                                    selection = TextRange(0, partidaLocal.length)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar partida",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = partidaFieldValue,
                            onValueChange = { partidaFieldValue = it },
                            label = { Text("Partida", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(partidaFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { 
                                partidaLocal = partidaFieldValue.text
                                partidaEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar partida",
                                tint = Color.White
                            )
                        }
                    }
                    
                    LaunchedEffect(partidaEditable) {
                        if (partidaEditable) {
                            partidaFocusRequester.requestFocus()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo Ubicación
                if (!ubicacionEditable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Transparent)
                                .padding(horizontalPadding / 2)
                        ) {
                            Text(
                                text = "Ubicación:",
                                color = Color.White,
                                fontSize = bodyFontSize
                            )
                        }
                        // No mostrar ícono de cámara cuando el campo está vacío
                        IconButton(
                            onClick = { 
                                ubicacionEditable = true
                                ubicacionFieldValue = TextFieldValue(
                                    text = ubicacionLocal,
                                    selection = TextRange(0, ubicacionLocal.length)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar ubicación",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = ubicacionFieldValue,
                            onValueChange = { ubicacionFieldValue = it },
                            label = { Text("Ubicación", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { 
                                ubicacionLocal = ubicacionFieldValue.text
                                ubicacionEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar ubicación",
                                tint = Color.White
                            )
                        }
                    }
                    
                    LaunchedEffect(ubicacionEditable) {
                        if (ubicacionEditable) {
                            ubicacionFocusRequester.requestFocus()
                        }
                    }
                }
            } else {
                // 3. Mostrar ambos campos (completados) con opciones de re-edición
                // Campo Partida (completado)
                if (!partidaEditable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Transparent)
                                .padding(horizontalPadding / 2)
                        ) {
                            Text(
                                text = "Partida: $partidaLocal",
                                color = Color.White,
                                fontSize = bodyFontSize
                            )
                        }
                        IconButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    onStockearClick("producto")
                                } else {
                                    launcher.launch(android.Manifest.permission.CAMERA)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Escanear partida",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { 
                                partidaEditable = true
                                partidaFieldValue = TextFieldValue(
                                    text = partidaLocal,
                                    selection = TextRange(0, partidaLocal.length)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar partida",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = partidaFieldValue,
                            onValueChange = { partidaFieldValue = it },
                            label = { Text("Partida", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(partidaFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { 
                                partidaLocal = partidaFieldValue.text
                                partidaEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar partida",
                                tint = Color.White
                            )
                        }
                    }
                    
                    LaunchedEffect(partidaEditable) {
                        if (partidaEditable) {
                            partidaFocusRequester.requestFocus()
                        }
                    }
                }

                // Campo Ubicación (completado)
                if (!ubicacionEditable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Transparent)
                                .padding(horizontalPadding / 2)
                        ) {
                            Text(
                                text = "Ubicación: $ubicacionLocal",
                                color = Color.White,
                                fontSize = bodyFontSize
                            )
                        }
                        IconButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    onStockearClick("ubicacion")
                                } else {
                                    launcher.launch(android.Manifest.permission.CAMERA)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Escanear ubicación",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { 
                                ubicacionEditable = true
                                ubicacionFieldValue = TextFieldValue(
                                    text = ubicacionLocal,
                                    selection = TextRange(0, ubicacionLocal.length)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar ubicación",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = ubicacionFieldValue,
                            onValueChange = { ubicacionFieldValue = it },
                            label = { Text("Ubicación", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { 
                                ubicacionLocal = ubicacionFieldValue.text
                                ubicacionEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar ubicación",
                                tint = Color.White
                            )
                        }
                    }
                    
                    LaunchedEffect(ubicacionEditable) {
                        if (ubicacionEditable) {
                            ubicacionFocusRequester.requestFocus()
                        }
                    }
                }
            }
            
            // Listado de ubicaciones (no dropdown)
            if (ubicaciones.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(formWidth)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    ubicaciones.forEach { ubic ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (ubicacionSeleccionada == ubic.numero.toString()) Color(0xFFD1E9FF) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 4.dp)
                                .clickable { ubicacionSeleccionada = ubic.numero.toString() }
                        ) {
                            Text(ubic.nombre, modifier = Modifier.weight(1f))
                            if (ubicacionSeleccionada == ubic.numero.toString()) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = "Ubicación seleccionada",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }


            // Los botones de escaneo ya están integrados en la lógica secuencial arriba
        }
        
        // Mostrar error de envío si existe (justo arriba del botón)
        if (errorEnvio != null && partidaLocal.isNotBlank() && ubicacionLocal.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp) // Arriba del botón (56dp altura + 16dp padding + 8dp espacio)
            ) {
                ErrorMessage(
                    message = errorEnvio!!,
                    modifier = Modifier.fillMaxWidth(formWidth)
                )
            }
        }
        
        // Botones de escaneo en posición fija (misma ubicación que botón Enviar)
        if (partidaLocal.isBlank()) {
            // Botón Escanear Partida
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onStockearClick("producto")
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth(formWidth)
                        .height(buttonHeight)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Escanear",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Escanear Partida",
                            color = Color.Black,
                            fontSize = bodyFontSize
                        )
                    }
                }
            }
        } else if (ubicacionLocal.isBlank()) {
            // Botón Escanear Ubicación
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onStockearClick("ubicacion")
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth(formWidth)
                        .height(buttonHeight)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Ubicación",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Escanear Ubicación",
                            color = Color.Black,
                            fontSize = bodyFontSize
                        )
                    }
                }
            }
        }
        
        // Botón de enviar en la parte inferior de la pantalla
        if (partidaLocal.isNotBlank() && ubicacionLocal.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
            Button(
                onClick = {
                    if (!isLoading) {
                        errorEnvio = null
                        isLoading = true
                        
                        val ubicacionLimpia = ubicacionLocal
                        
                        val partidas = listOf(
                            EstibarPartida(
                                nombreUbicacion = ubicacionLimpia,
                                numPartida = partidaLocal
                            )
                        )
                        val fechaHora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
                        val request = EstibarPartidasRequest(
                            partidas = partidas,
                            fechaHora = fechaHora,
                            codDeposito = deposito,
                            observacion = ""
                        )
                        
                        AppLogger.logInfo("EstivacionScreen", "Iniciando estivación - Partida: $partidaLocal, Ubicación: $ubicacionLimpia, Depósito: $deposito")
                        
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val response = ApiClient.apiService.estibarPartidas(request).execute()
                                
                                if (response.isSuccessful) {
                                    val responseBody = response.body()?.string()
                                    AppLogger.logInfo("EstivacionScreen", "Estivación exitosa - Partida: $partidaLocal, Ubicación: $ubicacionLimpia, Response: $responseBody")
                                    
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        context.startActivity(Intent(context, EstivacionSuccessActivity::class.java))
                                        (context as? Activity)?.finish()
                                    }
                                } else {
                                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                                    AppLogger.logError(
                                        tag = "EstivacionScreen",
                                        message = "Error al estibar partida: code=${response.code()}, error=$errorBody, partida=$partidaLocal, ubicación=$ubicacionLimpia"
                                    )
                                    
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        errorEnvio = "No se pudo enviar la estivación. Intenta nuevamente."
                                    }
                                }
                            } catch (e: Exception) {
                                AppLogger.logError(
                                    tag = "EstivacionScreen",
                                    message = "Excepción al estibar partida: ${e.message}, partida=$partidaLocal, ubicación=$ubicacionLimpia",
                                    throwable = e
                                )
                                
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    errorEnvio = "No se pudo enviar la estivación por un problema de conexión."
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    disabledContainerColor = Color(0xFFCCCCCC)
                ),
                modifier = Modifier
                    .fillMaxWidth(formWidth)
                    .height(buttonHeight)
            ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Enviar", color = Color.Black, fontSize = bodyFontSize)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "EstivacionScreen Preview")
@Composable
fun EstivacionScreenPreview() {
    EstivacionScreen(
        onBack = {},
        onStockearClick = {},
        producto = "123456789",
        ubicacion = "A-01"
    )
}
