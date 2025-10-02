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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    // Recordar depósito entre pantallas
    val prefs = context.getSharedPreferences("QRCodeScannerPrefs", Context.MODE_PRIVATE)
    var deposito by remember { mutableStateOf(prefs.getString("savedDeposito", "") ?: "") }
    var depositoFieldValue by remember { mutableStateOf(TextFieldValue(deposito)) }
    var depositoEditable by remember { mutableStateOf(false) }
    val depositoFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(deposito) {
        prefs.edit().putString("savedDeposito", deposito).apply()
    }
    
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
    var observacion by remember { mutableStateOf("") }
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
            .background(Color.White)
            .padding(WindowInsets.systemBars.asPaddingValues()),
        contentAlignment = Alignment.Center
    ) {
        // Botón de volver en la esquina superior izquierda
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color(0xFF1976D2))
                }
                Spacer(Modifier.weight(1f))
            }
        }
        
        // Título centrado en la parte superior
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Text(
                text = "Estivación",
                fontSize = 24.sp,
                color = Color(0xFF1976D2),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 60.dp) // Add padding to account for title
                .verticalScroll(rememberScrollState())
        ) {
            // Campo depósito con ícono de edición o guardado
            if (!depositoEditable) {
                // Modo solo lectura - sin bordes, texto gris
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.Transparent)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (deposito.isBlank()) "Depósito:" else "Depósito: $deposito",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(
                        onClick = { 
                            depositoEditable = true
                            depositoFieldValue = TextFieldValue(
                                text = deposito,
                                selection = TextRange(0, deposito.length)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Editar depósito",
                            tint = Color(0xFF1976D2)
                        )
                    }
                }
            } else {
                // Modo edición - campo normal con ícono de guardar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    OutlinedTextField(
                        value = depositoFieldValue,
                        onValueChange = { depositoFieldValue = it },
                        label = { Text("Depósito", color = Color.Black) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(depositoFocusRequester),
                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Color.Black,
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color(0xFF1976D2),
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { 
                            deposito = depositoFieldValue.text
                            depositoEditable = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Guardar depósito",
                            tint = Color(0xFF1976D2)
                        )
                    }
                }
                
                // Auto-focus y selección cuando se habilita la edición
                LaunchedEffect(depositoEditable) {
                    if (depositoEditable) {
                        depositoFocusRequester.requestFocus()
                    }
                }
            }
            
            // Lógica secuencial: mostrar campos y botones según el estado
            if (partidaLocal.isBlank()) {
                // 1. Solo mostrar campo partida
                // Campo Partida con ícono de cámara y edición
                if (!partidaEditable) {
                    // Modo solo lectura - sin bordes, texto gris
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Transparent)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Partida:",
                                color = Color.Gray,
                                fontSize = 16.sp
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
                                tint = Color(0xFF1976D2)
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
                                tint = Color(0xFF1976D2)
                            )
                        }
                    }
                } else {
                    // Modo edición - campo normal con ícono de guardar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = partidaFieldValue,
                            onValueChange = { partidaFieldValue = it },
                            label = { Text("Partida", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(partidaFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.Black),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = Color.Black,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.Black,
                                unfocusedLabelColor = Color.Black
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
                                tint = Color(0xFF1976D2)
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
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Transparent)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Partida: $partidaLocal",
                                color = Color.Gray,
                                fontSize = 16.sp
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
                                tint = Color(0xFF1976D2)
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
                                tint = Color(0xFF1976D2)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = partidaFieldValue,
                            onValueChange = { partidaFieldValue = it },
                            label = { Text("Partida", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(partidaFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.Black),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = Color.Black,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.Black,
                                unfocusedLabelColor = Color.Black
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
                                tint = Color(0xFF1976D2)
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
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Transparent)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Ubicación:",
                                color = Color.Gray,
                                fontSize = 16.sp
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
                                tint = Color(0xFF1976D2)
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
                                tint = Color(0xFF1976D2)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = ubicacionFieldValue,
                            onValueChange = { ubicacionFieldValue = it },
                            label = { Text("Ubicación", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.Black),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = Color.Black,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.Black,
                                unfocusedLabelColor = Color.Black
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
                                tint = Color(0xFF1976D2)
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
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Transparent)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Partida: $partidaLocal",
                                color = Color.Gray,
                                fontSize = 16.sp
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
                                tint = Color(0xFF1976D2)
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
                                tint = Color(0xFF1976D2)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = partidaFieldValue,
                            onValueChange = { partidaFieldValue = it },
                            label = { Text("Partida", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(partidaFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.Black),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = Color.Black,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.Black,
                                unfocusedLabelColor = Color.Black
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
                                tint = Color(0xFF1976D2)
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

                // Campo Ubicación (completado)
                if (!ubicacionEditable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Transparent)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Ubicación: $ubicacionLocal",
                                color = Color.Gray,
                                fontSize = 16.sp
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
                                tint = Color(0xFF1976D2)
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
                                tint = Color(0xFF1976D2)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = ubicacionFieldValue,
                            onValueChange = { ubicacionFieldValue = it },
                            label = { Text("Ubicación", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.Black),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = Color.Black,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.Black,
                                unfocusedLabelColor = Color.Black
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
                                tint = Color(0xFF1976D2)
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
            
            // Continuar con la lógica de botones grandes integrados
            if (partidaLocal.isBlank()) {
                // 1. Solo mostrar botón escanear partida si no hay partida
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onStockearClick("producto")
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Stockear",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Escanear Partida",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            } else if (ubicacionLocal.isBlank()) {
                // 2. Mostrar botón escanear ubicación si hay partida pero no ubicación
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onStockearClick("ubicacion")
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Stockear",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Escanear Ubicación",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            } 
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Cerrar la lógica secuencial
            
            // Listado de ubicaciones (no dropdown)
            if (ubicaciones.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
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
                                    tint = Color(0xFF1976D2)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Los botones de escaneo ya están integrados en la lógica secuencial arriba


           if(partidaLocal.isNotBlank() && ubicacionLocal.isNotBlank())
           {// Campo editable para observación
            OutlinedTextField(
                value = observacion,
                onValueChange = { observacion = it },
                label = { Text("Observación") },
                singleLine = false,
                modifier = Modifier.fillMaxWidth(0.8f),
                textStyle = LocalTextStyle.current.copy(color = Color.Black),
                colors = TextFieldDefaults.outlinedTextFieldColors(

                    cursorColor = Color.Black,
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color(0xFF1976D2),
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black
                )
            )
           }
        }
        // Botón de enviar en la parte inferior del scroll
        if (partidaLocal.isNotBlank() && ubicacionLocal.isNotBlank()) {
            Spacer(modifier = Modifier.height(32.dp))
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
                        val usuario = prefs.getString("savedUser", "") ?: ""
                        val obsFinal = if (observacion.isNotBlank()) "$usuario: $observacion" else usuario
                        val request = EstibarPartidasRequest(
                            partidas = partidas,
                            fechaHora = fechaHora,
                            codDeposito = deposito,
                            observacion = obsFinal
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
                    containerColor = Color(0xFF1976D2),
                    disabledContainerColor = Color(0xFF90CAF9)
                ),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Enviar", color = Color.White, fontSize = 16.sp)
                }
            }
            if (errorEnvio != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorEnvio ?: "", color = Color.Red, modifier = Modifier.fillMaxWidth(0.8f))
            }
            Spacer(modifier = Modifier.height(32.dp))
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
