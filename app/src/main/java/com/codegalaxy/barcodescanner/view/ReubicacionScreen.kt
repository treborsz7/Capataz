import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.RequestBody

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
    LaunchedEffect(deposito) {
        prefs.edit().putString("savedDeposito", deposito).apply()
    }
    var productos by remember { mutableStateOf(listOf<String>()) }
    var ubicacionOrigenActual by remember { mutableStateOf<String?>(null) }
    var ubicacionDestinoActual by remember { mutableStateOf<String?>(null) }
    var observacion by remember { mutableStateOf("") }
    var errorEnvio by remember { mutableStateOf<String?>(null) }
    var reubicaciones by remember { mutableStateOf(listOf<Triple<String, String, String>>()) }

    // Manejo de escaneo
    LaunchedEffect(producto) {
        if (!producto.isNullOrBlank() && !productos.contains(producto)) {
            productos = productos + producto
        }
    }
    LaunchedEffect(ubicacionOrigen) {
        if (!ubicacionOrigen.isNullOrBlank()) {
            ubicacionOrigenActual = ubicacionOrigen
        }
    }
    LaunchedEffect(ubicacionDestino) {
        if (!ubicacionDestino.isNullOrBlank()) {
            ubicacionDestinoActual = ubicacionDestino
        }
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
        ) {
            // Campo depósito
            OutlinedTextField(
                value = deposito,
                onValueChange = { deposito = it },
                label = { Text("Depósito", color = Color.Black) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f),
                textStyle = LocalTextStyle.current.copy(color = Color.Black)
            )
            // Botón escanear producto
            if (productos.isEmpty()) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Agregar producto",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Escanear Producto",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Text("Productos")
                    productos.forEach { prod ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(prod, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                productos = productos.filter { it != prod }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Eliminar producto",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
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
                        .height(40.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Agregar Producto", color = Color.White, fontSize = 16.sp)
                }
            }
            // Botón escanear ubicación origen
            if (ubicacionOrigenActual.isNullOrBlank()) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Ubicación Origen",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ubicación Origen",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Text("Ubicación Origen", modifier = Modifier.weight(1f))
                    Text(ubicacionOrigenActual ?: "-", modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        if (hasCameraPermission) {
                            onReubicarClick("ubicacion_origen")
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refrescar ubicación origen",
                            tint = Color(0xFF1976D2)
                        )
                    }
                }
            }
            // Botón escanear ubicación destino
            if (ubicacionDestinoActual.isNullOrBlank()) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Ubicación Destino",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ubicación Destino",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Text("Ubicación Destino", modifier = Modifier.weight(1f))
                    Text(ubicacionDestinoActual ?: "-", modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        if (hasCameraPermission) {
                            onReubicarClick("ubicacion_destino")
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refrescar ubicación destino",
                            tint = Color(0xFF1976D2)
                        )
                    }
                }
            }
            // Botón agregar reubicación
            Button(
                onClick = {
                    if (!productos.isNullOrEmpty() && !ubicacionOrigenActual.isNullOrBlank() && !ubicacionDestinoActual.isNullOrBlank()) {
                        reubicaciones = reubicaciones + Triple(ubicacionOrigenActual!!, ubicacionDestinoActual!!, productos.first())
                        productos = listOf()
                        ubicacionOrigenActual = null
                        ubicacionDestinoActual = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(40.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("Agregar Reubicación", color = Color.White, fontSize = 16.sp)
            }
            // Listado de reubicaciones
            if (reubicaciones.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    reubicaciones.forEach { (origen, destino, partida) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$partida: $origen → $destino", modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                reubicaciones = reubicaciones.filterNot { it == Triple(origen, destino, partida) }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Eliminar reubicación",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
            // Campo observación
            OutlinedTextField(
                value = observacion,
                onValueChange = { observacion = it },
                label = { Text("Observación") },
                singleLine = false,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
        // Botón de enviar en la esquina inferior derecha
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (reubicaciones.isNotEmpty() && deposito.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            errorEnvio = null
                            val fechaHora = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date())
                            val json = JSONObject()
                            val arr = JSONArray()
                            val partidas = reubicaciones.map { (origen, destino, partida) ->
                                ReubicarPartida(
                                    nombreUbiOrigen = origen ?: "",
                                    nombreUbiDestino= destino ?: "",
                                    numPartida = partida
                                )
                            }
                            val request = ReubicarPartidasRequest(
                                reubicaciones = partidas,
                                fechaHora = fechaHora,
                                codDeposito = deposito,
                                observacion = ""
                            )
                           // val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val response = ApiClient.apiService.reubicarPartidas(request).execute()
                                    if (response.isSuccessful) {
                                        withContext(Dispatchers.Main) {
                                            reubicaciones = listOf()
                                            errorEnvio = null
                                        }
                                    } else {
                                        val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                                        withContext(Dispatchers.Main) {
                                            errorEnvio = errorBody
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        errorEnvio = e.message ?: "Error desconocido"
                                    }
                                }
                            }
                        },
                        enabled = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp)
                    ) {
                        Text("Enviar", color = Color.White)
                    }
                    if (errorEnvio != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorEnvio ?: "", color = Color.Red, modifier = Modifier.fillMaxWidth(0.8f))
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
