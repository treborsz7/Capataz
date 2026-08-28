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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.thinkthat.mamusckascaner.presentation.reubicacion.ReubicacionPantalla
import com.thinkthat.mamusckascaner.presentation.reubicacion.ReubicacionUiState
import com.thinkthat.mamusckascaner.view.components.ErrorMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReubicacionScreen(
    state: ReubicacionUiState,
    onBack: () -> Unit = {},
    onReubicarClick: (boton: String) -> Unit = {},
    onProductoChange: (String) -> Unit = {},
    onUbicacionOrigenChange: (String) -> Unit = {},
    onUbicacionDestinoChange: (String) -> Unit = {},
    onEnviar: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    // La pantalla solo dibuja: los datos y el estado de envío vienen del ViewModel.
    val producto: String? = state.partida.takeIf { it.isNotBlank() }
    val ubicacionOrigen: String? = state.ubicacionOrigen.takeIf { it.isNotBlank() }
    val ubicacionDestino: String? = state.ubicacionDestino.takeIf { it.isNotBlank() }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // Responsive values based on screen size
    val horizontalPadding = maxOf(minOf(screenWidth * 0.08f, 32.dp), 16.dp)
    val logoSize = maxOf(minOf(screenWidth * 0.6f, 350.dp), 200.dp)
    val formWidth = 0.85f
    val topOffset = -maxOf(minOf(screenHeight * 0.05f, 40.dp), 20.dp)
    val buttonHeight = maxOf(minOf(screenHeight * 0.07f, 64.dp), 48.dp)
    val verticalSpacing = maxOf(minOf(screenHeight * 0.015f, 16.dp), 8.dp)
    val errorBottomPadding = maxOf(minOf(screenHeight * 0.12f, 100.dp), 60.dp)
    
    // Font sizes - usando maxOf/minOf para TextUnit
    val titleFontSize = maxOf(minOf((screenWidth * 0.06f).value, 28f), 20f).sp
    val bodyFontSize = maxOf(minOf((screenWidth * 0.04f).value, 18f), 14f).sp
    
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
    val deposito = state.codDeposito

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
    
    val errorEnvio = state.error
    val isLoading = state.isEnviando
    var showBackDialog by remember { mutableStateOf(false) }
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
    
    // Notificar cambios al Activity
    LaunchedEffect(productoLocal) {
        if (productoLocal.isNotBlank()) {
            onProductoChange(productoLocal)
        }
    }
    
    LaunchedEffect(ubicacionOrigenLocal) {
        if (ubicacionOrigenLocal.isNotBlank()) {
            onUbicacionOrigenChange(ubicacionOrigenLocal)
        }
    }
    
    LaunchedEffect(ubicacionDestinoLocal) {
        if (ubicacionDestinoLocal.isNotBlank()) {
            onUbicacionDestinoChange(ubicacionDestinoLocal)
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
                .padding(horizontalPadding / 2)
        ) {
            IconButton(onClick = { showBackDialog = true }) {
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
                text = "Reubicar",
                fontSize = titleFontSize,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
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
            if (productoLocal.isBlank()) {
                // 1. Mostrar campo producto + botón escanear producto
                if (!productoEditable) {
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
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = productoFieldValue,
                            onValueChange = { productoFieldValue = it },
                            label = { Text("Partida", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(productoFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
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
                
                // Botón Escanear Producto movido a posición fija al final
            } else if (ubicacionOrigenLocal.isBlank()) {
                // 2. Mostrar campo producto (completado) + campo ubicación origen + botón escanear ubicación origen
                if (!productoEditable) {
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
                                text = "Partida: $productoLocal",
                                color = Color.White,
                                fontSize = bodyFontSize
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
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = productoFieldValue,
                            onValueChange = { productoFieldValue = it },
                            label = { Text("Partida", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(productoFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
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

                if (!ubicacionOrigenEditable) {
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
                                text = "Ubicación Origen:",
                                color = Color.White,
                                fontSize = bodyFontSize
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
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = ubicacionOrigenFieldValue,
                            onValueChange = { ubicacionOrigenFieldValue = it },
                            label = { Text("Ubicación Origen", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionOrigenFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
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
                
                // Botón Escanear Ubicación Origen movido a posición fija al final
            } else if (ubicacionDestinoLocal.isBlank()) {
                // 3. Mostrar campos producto y ubicación origen (completados) + campo ubicación destino + botón escanear ubicación destino
                if (!productoEditable) {
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
                                text = "Partida: $productoLocal",
                                color = Color.White,
                                fontSize = bodyFontSize
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
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = productoFieldValue,
                            onValueChange = { productoFieldValue = it },
                            label = { Text("Partida", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(productoFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
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

                if (!ubicacionOrigenEditable) {
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
                                text = "Ubicación Origen: $ubicacionOrigenLocal",
                                color = Color.White,
                                fontSize = bodyFontSize
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
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = ubicacionOrigenFieldValue,
                            onValueChange = { ubicacionOrigenFieldValue = it },
                            label = { Text("Ubicación Origen", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionOrigenFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
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

                if (!ubicacionDestinoEditable) {
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
                                text = "Ubicación Destino:",
                                color = Color.White,
                                fontSize = bodyFontSize
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
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = ubicacionDestinoFieldValue,
                            onValueChange = { ubicacionDestinoFieldValue = it },
                            label = { Text("Ubicación Destino", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionDestinoFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
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
                
                // Botón Escanear Ubicación Destino movido a posición fija al final
            } else {
                // 4. Mostrar todos los campos (completados) con botones de edición/escaneo
                if (!productoEditable) {
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
                                text = "Partida: $productoLocal",
                                color = Color.White,
                                fontSize = bodyFontSize
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
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = productoFieldValue,
                            onValueChange = { productoFieldValue = it },
                            label = { Text("Partida", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(productoFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
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

                if (!ubicacionOrigenEditable) {
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
                                text = "Ubicación Origen: $ubicacionOrigenLocal",
                                color = Color.White,
                                fontSize = bodyFontSize
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
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = ubicacionOrigenFieldValue,
                            onValueChange = { ubicacionOrigenFieldValue = it },
                            label = { Text("Ubicación Origen", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionOrigenFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
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

                if (!ubicacionDestinoEditable) {
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
                                text = "Ubicación Destino: $ubicacionDestinoLocal",
                                color = Color.White,
                                fontSize = bodyFontSize
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
                        modifier = Modifier.fillMaxWidth(formWidth)
                    ) {
                        OutlinedTextField(
                            value = ubicacionDestinoFieldValue,
                            onValueChange = { ubicacionDestinoFieldValue = it },
                            label = { Text("Ubicación Destino", color = Color.White) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(ubicacionDestinoFocusRequester),
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                cursorColor = Color.White,
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF1976D2),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
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

            // Los botones de escaneo ya están integrados en la lógica secuencial arriba
        }
        
        // Botones de escaneo en posición fija (misma ubicación que botón Enviar)
        if (productoLocal.isBlank()) {
            // Botón Escanear Producto
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = horizontalPadding / 2)
            ) {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onReubicarClick("producto")
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
                            text = "Escanear Producto",
                            color = Color.Black,
                            fontSize = bodyFontSize
                        )
                    }
                }
            }
        } else if (ubicacionOrigenLocal.isBlank()) {
            // Botón Escanear Ubicación Origen
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = horizontalPadding / 2)
            ) {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onReubicarClick("ubicacion_origen")
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
                            contentDescription = "Ubicación Origen",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Escanear Ubicación Origen",
                            color = Color.Black,
                            fontSize = bodyFontSize
                        )
                    }
                }
            }
        } else if (ubicacionDestinoLocal.isBlank()) {
            // Botón Escanear Ubicación Destino
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = horizontalPadding / 2)
            ) {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onReubicarClick("ubicacion_destino")
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
                            contentDescription = "Ubicación Destino",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Escanear Ubicación Destino",
                            color = Color.Black,
                            fontSize = bodyFontSize
                        )
                    }
                }
            }
        }
           
        // Botón de enviar en la parte inferior de la pantalla
        if (productoLocal.isNotBlank() && ubicacionOrigenLocal.isNotBlank() && ubicacionDestinoLocal.isNotBlank() && deposito.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = horizontalPadding / 2)
            ) {
                Button(
                    onClick = onEnviar,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        disabledContainerColor = Color(0xFFE0E0E0)
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
        
        // Mostrar error de envío si existe (superpuesto sobre el botón)
        if (errorEnvio != null && productoLocal.isNotBlank() && ubicacionOrigenLocal.isNotBlank() && ubicacionDestinoLocal.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = horizontalPadding / 2)
            ) {
                ErrorMessage(
                    message = errorEnvio!!,
                    modifier = Modifier.fillMaxWidth(formWidth),
                    onDismiss = onDismissError
                )
            }
        }
        
        // Diálogo de confirmación para volver
        if (showBackDialog) {
            AlertDialog(
                onDismissRequest = { showBackDialog = false },
                title = { Text("Volver al menú") },
                text = { Text("¿Estás seguro de que quieres volver al menú principal?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBackDialog = false
                            onBack()
                        }
                    ) {
                        Text("Sí")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showBackDialog = false }
                    ) {
                        Text("No")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "ReubicacionScreen Preview")
@Composable
fun ReubicacionScreenPreview() {
    ReubicacionScreen(
        state = ReubicacionUiState(
            pantalla = ReubicacionPantalla.FORMULARIO,
            partida = "123456789",
            ubicacionOrigen = "A-01",
            ubicacionDestino = "B-02",
            codDeposito = "3B"
        )
    )
}

