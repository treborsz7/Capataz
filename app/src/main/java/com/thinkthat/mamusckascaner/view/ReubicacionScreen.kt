import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.thinkthat.mamusckascaner.service.Services.ApiClient
import com.thinkthat.mamusckascaner.service.Services.ReubicarPartida
import com.thinkthat.mamusckascaner.service.Services.ReubicarPartidasRequest
import com.thinkthat.mamusckascaner.view.EstivacionSuccessActivity
import com.thinkthat.mamusckascaner.utils.AppLogger
import com.thinkthat.mamusckascaner.view.components.ErrorMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReubicacionScreen(
    onBack: () -> Unit = {},
    onReubicarClick: (boton: String) -> Unit = {},
    producto: String? = null,
    ubicacionOrigen: String? = null,
    ubicacionDestino: String? = null
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
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }
    // Persistencia de depósito
    val prefs = context.getSharedPreferences("QRCodeScannerPrefs", android.content.Context.MODE_PRIVATE)
    var deposito by remember { mutableStateOf(prefs.getString("savedDeposito", "") ?: "") }
    var depositoFieldValue by remember { mutableStateOf(TextFieldValue(deposito)) }
    var depositoEditable by remember { mutableStateOf(false) }
    val depositoFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(deposito) {
        prefs.edit().putString("savedDeposito", deposito).apply()
    }
    
    // Estados para campos editables
    var productoLocal by remember { mutableStateOf(producto ?: "") }
    var productoEditable by remember { mutableStateOf(false) }
    var productoFieldValue by remember { mutableStateOf(TextFieldValue(producto ?: "")) }
    val productoFocusRequester = remember { FocusRequester() }
    
    var ubicacionOrigenLocal by remember { mutableStateOf(ubicacionOrigen ?: "") }
    var ubicacionOrigenEditable by remember { mutableStateOf(false) }
    var ubicacionOrigenFieldValue by remember { mutableStateOf(TextFieldValue(ubicacionOrigen ?: "")) }
    val ubicacionOrigenFocusRequester = remember { FocusRequester() }
    
    var ubicacionDestinoLocal by remember { mutableStateOf(ubicacionDestino ?: "") }
    var ubicacionDestinoEditable by remember { mutableStateOf(false) }
    var ubicacionDestinoFieldValue by remember { mutableStateOf(TextFieldValue(ubicacionDestino ?: "")) }
    val ubicacionDestinoFocusRequester = remember { FocusRequester() }
    
    var errorEnvio by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    // Sincronización de valores de la cámara
    LaunchedEffect(producto) {
        if (producto != null) {
            productoLocal = producto
            productoFieldValue = TextFieldValue(producto)
            // Salir del modo editable si estaba activo
            productoEditable = false
        }
    }
    LaunchedEffect(ubicacionOrigen) {
        if (ubicacionOrigen != null) {
            ubicacionOrigenLocal = ubicacionOrigen
            ubicacionOrigenFieldValue = TextFieldValue(ubicacionOrigen)
            // Salir del modo editable si estaba activo
            ubicacionOrigenEditable = false
        }
    }
    LaunchedEffect(ubicacionDestino) {
        if (ubicacionDestino != null) {
            ubicacionDestinoLocal = ubicacionDestino
            ubicacionDestinoFieldValue = TextFieldValue(ubicacionDestino)
            // Salir del modo editable si estaba activo
            ubicacionDestinoEditable = false
        }
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
                .padding(16.dp)
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
                .padding(top = 16.dp)
        ) {
            Text(
                text = "Reubicar",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 80.dp)
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
                            color = Color.White,
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
                            tint = Color.White
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
                            tint = Color.White
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
            if (productoLocal.isBlank()) {
                // 1. Mostrar campo producto + botón escanear producto
                if (!productoEditable) {
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
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        // No mostrar ícono de cámara cuando el campo está vacío
                        IconButton(
                            onClick = { 
                                productoEditable = true
                                productoFieldValue = TextFieldValue(
                                    text = productoLocal,
                                    selection = TextRange(0, productoLocal.length)
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
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = productoFieldValue,
                            onValueChange = { productoFieldValue = it },
                            label = { Text("Partida", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(productoFocusRequester),
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
                                productoLocal = productoFieldValue.text
                                productoEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar partida",
                                tint = Color.White
                            )
                        }
                    }
                    
                    LaunchedEffect(productoEditable) {
                        if (productoEditable) {
                            productoFocusRequester.requestFocus()
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onReubicarClick("producto")
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Escanear producto",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Escanear Producto",
                            color = Color.Black,
                            fontSize = 18.sp
                        )
                    }
                }
            } else if (ubicacionOrigenLocal.isBlank()) {
                // 2. Mostrar campo producto (completado) + campo ubicación origen + botón escanear ubicación origen
                if (!productoEditable) {
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
                                text = "Partida: $productoLocal",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    onReubicarClick("producto")
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
                                productoEditable = true
                                productoFieldValue = TextFieldValue(
                                    text = productoLocal,
                                    selection = TextRange(0, productoLocal.length)
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
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = productoFieldValue,
                            onValueChange = { productoFieldValue = it },
                            label = { Text("Partida", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(productoFocusRequester),
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
                                productoLocal = productoFieldValue.text
                                productoEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar partida",
                                tint = Color.White
                            )
                        }
                    }
                    
                    LaunchedEffect(productoEditable) {
                        if (productoEditable) {
                            productoFocusRequester.requestFocus()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!ubicacionOrigenEditable) {
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
                                text = "Ubicación Origen:",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        // No mostrar ícono de cámara cuando el campo está vacío
                        IconButton(
                            onClick = { 
                                ubicacionOrigenEditable = true
                                ubicacionOrigenFieldValue = TextFieldValue(
                                    text = ubicacionOrigenLocal,
                                    selection = TextRange(0, ubicacionOrigenLocal.length)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar ubicación origen",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = ubicacionOrigenFieldValue,
                            onValueChange = { ubicacionOrigenFieldValue = it },
                            label = { Text("Ubicación Origen", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionOrigenFocusRequester),
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
                                ubicacionOrigenLocal = ubicacionOrigenFieldValue.text
                                ubicacionOrigenEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar ubicación origen",
                                tint = Color.White
                            )
                        }
                    }
                    
                    LaunchedEffect(ubicacionOrigenEditable) {
                        if (ubicacionOrigenEditable) {
                            ubicacionOrigenFocusRequester.requestFocus()
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onReubicarClick("ubicacion_origen")
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Ubicación Origen",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Escanear Ubicación Origen",
                            color = Color.Black,
                            fontSize = 18.sp
                        )
                    }
                }
            } else if (ubicacionDestinoLocal.isBlank()) {
                // 3. Mostrar campos producto y ubicación origen (completados) + campo ubicación destino + botón escanear ubicación destino
                if (!productoEditable) {
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
                                text = "Partida: $productoLocal",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    onReubicarClick("producto")
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
                                productoEditable = true
                                productoFieldValue = TextFieldValue(
                                    text = productoLocal,
                                    selection = TextRange(0, productoLocal.length)
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
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = productoFieldValue,
                            onValueChange = { productoFieldValue = it },
                            label = { Text("Partida", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(productoFocusRequester),
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
                                productoLocal = productoFieldValue.text
                                productoEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar partida",
                                tint = Color.White
                            )
                        }
                    }
                    
                    LaunchedEffect(productoEditable) {
                        if (productoEditable) {
                            productoFocusRequester.requestFocus()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!ubicacionOrigenEditable) {
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
                                text = "Ubicación Origen: $ubicacionOrigenLocal",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    onReubicarClick("ubicacion_origen")
                                } else {
                                    launcher.launch(android.Manifest.permission.CAMERA)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Escanear ubicación origen",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { 
                                ubicacionOrigenEditable = true
                                ubicacionOrigenFieldValue = TextFieldValue(
                                    text = ubicacionOrigenLocal,
                                    selection = TextRange(0, ubicacionOrigenLocal.length)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar ubicación origen",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = ubicacionOrigenFieldValue,
                            onValueChange = { ubicacionOrigenFieldValue = it },
                            label = { Text("Ubicación Origen", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionOrigenFocusRequester),
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
                                ubicacionOrigenLocal = ubicacionOrigenFieldValue.text
                                ubicacionOrigenEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar ubicación origen",
                                tint = Color.White
                            )
                        }
                    }
                    
                    LaunchedEffect(ubicacionOrigenEditable) {
                        if (ubicacionOrigenEditable) {
                            ubicacionOrigenFocusRequester.requestFocus()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!ubicacionDestinoEditable) {
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
                                text = "Ubicación Destino:",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        // No mostrar ícono de cámara cuando el campo está vacío
                        IconButton(
                            onClick = { 
                                ubicacionDestinoEditable = true
                                ubicacionDestinoFieldValue = TextFieldValue(
                                    text = ubicacionDestinoLocal,
                                    selection = TextRange(0, ubicacionDestinoLocal.length)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar ubicación destino",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = ubicacionDestinoFieldValue,
                            onValueChange = { ubicacionDestinoFieldValue = it },
                            label = { Text("Ubicación Destino", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionDestinoFocusRequester),
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
                                ubicacionDestinoLocal = ubicacionDestinoFieldValue.text
                                ubicacionDestinoEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar ubicación destino",
                                tint = Color.White
                            )
                        }
                    }
                    
                    LaunchedEffect(ubicacionDestinoEditable) {
                        if (ubicacionDestinoEditable) {
                            ubicacionDestinoFocusRequester.requestFocus()
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onReubicarClick("ubicacion_destino")
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Ubicación Destino",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Escanear Ubicación Destino",
                            color = Color.Black,
                            fontSize = 18.sp
                        )
                    }
                }
            } else {
                // 4. Mostrar todos los campos (completados) con botones de edición/escaneo
                if (!productoEditable) {
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
                                text = "Partida: $productoLocal",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    onReubicarClick("producto")
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
                                productoEditable = true
                                productoFieldValue = TextFieldValue(
                                    text = productoLocal,
                                    selection = TextRange(0, productoLocal.length)
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
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = productoFieldValue,
                            onValueChange = { productoFieldValue = it },
                            label = { Text("Partida", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(productoFocusRequester),
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
                                productoLocal = productoFieldValue.text
                                productoEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar partida",
                                tint = Color.White
                            )
                        }
                    }
                    
                    LaunchedEffect(productoEditable) {
                        if (productoEditable) {
                            productoFocusRequester.requestFocus()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!ubicacionOrigenEditable) {
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
                                text = "Ubicación Origen: $ubicacionOrigenLocal",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    onReubicarClick("ubicacion_origen")
                                } else {
                                    launcher.launch(android.Manifest.permission.CAMERA)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Escanear ubicación origen",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { 
                                ubicacionOrigenEditable = true
                                ubicacionOrigenFieldValue = TextFieldValue(
                                    text = ubicacionOrigenLocal,
                                    selection = TextRange(0, ubicacionOrigenLocal.length)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar ubicación origen",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = ubicacionOrigenFieldValue,
                            onValueChange = { ubicacionOrigenFieldValue = it },
                            label = { Text("Ubicación Origen", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionOrigenFocusRequester),
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
                                ubicacionOrigenLocal = ubicacionOrigenFieldValue.text
                                ubicacionOrigenEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar ubicación origen",
                                tint = Color.White
                            )
                        }
                    }
                    
                    LaunchedEffect(ubicacionOrigenEditable) {
                        if (ubicacionOrigenEditable) {
                            ubicacionOrigenFocusRequester.requestFocus()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!ubicacionDestinoEditable) {
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
                                text = "Ubicación Destino: $ubicacionDestinoLocal",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    onReubicarClick("ubicacion_destino")
                                } else {
                                    launcher.launch(android.Manifest.permission.CAMERA)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Escanear ubicación destino",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { 
                                ubicacionDestinoEditable = true
                                ubicacionDestinoFieldValue = TextFieldValue(
                                    text = ubicacionDestinoLocal,
                                    selection = TextRange(0, ubicacionDestinoLocal.length)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar ubicación destino",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        OutlinedTextField(
                            value = ubicacionDestinoFieldValue,
                            onValueChange = { ubicacionDestinoFieldValue = it },
                            label = { Text("Ubicación Destino", color = Color.Black) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionDestinoFocusRequester),
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
                                ubicacionDestinoLocal = ubicacionDestinoFieldValue.text
                                ubicacionDestinoEditable = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar ubicación destino",
                                tint = Color.White
                            )
                        }
                    }
                    
                    LaunchedEffect(ubicacionDestinoEditable) {
                        if (ubicacionDestinoEditable) {
                            ubicacionDestinoFocusRequester.requestFocus()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Los botones de escaneo ya están integrados en la lógica secuencial arriba
        }
        
        // Mostrar error de envío si existe (justo arriba del botón)
        if (errorEnvio != null && productoLocal.isNotBlank() && ubicacionOrigenLocal.isNotBlank() && ubicacionDestinoLocal.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp) // Arriba del botón (56dp altura + 16dp padding + 8dp espacio)
            ) {
                ErrorMessage(
                    message = errorEnvio!!,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
        }
           
        // Botón de enviar en la parte inferior de la pantalla
        if (productoLocal.isNotBlank() && ubicacionOrigenLocal.isNotBlank() && ubicacionDestinoLocal.isNotBlank() && deposito.isNotBlank()) {
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
                            
                            val fechaHora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(
                                Date()
                            )

                            val reubicacion = ReubicarPartida(
                                    nombreUbiOrigen = ubicacionOrigenLocal,
                                    nombreUbiDestino = ubicacionDestinoLocal,
                                    numPartida = productoLocal
                                )

                            val request = ReubicarPartidasRequest(
                                reubicaciones = mutableListOf(reubicacion),
                                fechaHora = fechaHora,
                                codDeposito = deposito,
                                observacion = "",
                                reubicacion = 0
                            )

                            AppLogger.logInfo("ReubicacionScreen", "Iniciando reubicación - Partida: $productoLocal, Origen: $ubicacionOrigenLocal, Destino: $ubicacionDestinoLocal, Depósito: $deposito")
                            
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    AppLogger.logInfo("ReubicacionScreen", "Enviando request a ApiClient.apiService.reubicarPartidas...")
                                    
                                    val response = ApiClient.apiService.reubicarPartidas(request).execute()
                                    
                                    AppLogger.logInfo("ReubicacionScreen", "Respuesta recibida - Código: ${response.code()}, Exitoso: ${response.isSuccessful}")
                                    
                                    if (response.isSuccessful) {
                                        val responseBody = response.body()?.string() ?: "Sin contenido"
                                        AppLogger.logInfo("ReubicacionScreen", "Respuesta exitosa: $responseBody")
                                        AppLogger.logInfo("ReubicacionScreen", "✅ Reubicación completada exitosamente - Partida: $productoLocal")
                                        
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            errorEnvio = null
                                            context.startActivity(Intent(context, EstivacionSuccessActivity::class.java))
                                            (context as? Activity)?.finish()
                                        }
                                    } else {
                                        val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                                        AppLogger.logError(
                                            tag = "ReubicacionScreen",
                                            message = "Error en respuesta: code=${response.code()} body=$errorBody"
                                        )
                                        
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            errorEnvio = "No se pudo reubicar la partida. Intenta nuevamente."
                                        }
                                    }
                                } catch (e: Exception) {
                                    AppLogger.logError(
                                        tag = "ReubicacionScreen",
                                        message = "Excepción durante el envío: ${e.message}",
                                        throwable = e
                                    )
                                    
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        errorEnvio = "No se pudo reubicar la partida por un problema de conexión."
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Enviar", color = Color.Black, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "ReubicacionScreen Preview")
@Composable
fun ReubicacionScreenPreview() {
    ReubicacionScreen(
        onBack = {},
        onReubicarClick = {},
        producto = "123456789",
        ubicacionOrigen = "A-01",
        ubicacionDestino = "B-02"
    )
}

