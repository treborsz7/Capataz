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
import com.codegalaxy.barcodescanner.view.EstivacionSuccessActivity
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    LaunchedEffect(deposito) {
        prefs.edit().putString("savedDeposito", deposito).apply()
    }
    // Estado para productos (lista)
    var productos by remember { mutableStateOf(listOf<String>()) }
    // Si viene un producto por parámetro (de la cámara), agregarlo a la lista
    LaunchedEffect(producto) {
        if (!producto.isNullOrBlank() && !productos.contains(producto)) {
            productos = productos + producto
        }
    }
    // Estado para ubicaciones
    var ubicaciones by remember { mutableStateOf(listOf<UbicacionResponse>()) }
    //var ubicacionSeleccionada by remember { mutableStateOf<String?>(null) }
    var expandedUbicaciones by remember { mutableStateOf(false) }
    var observacion by remember { mutableStateOf("") }
    var ubicacionSeleccionada by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
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
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.align(Alignment.Center)
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
            // Si la lista de productos está vacía, mostrar botón escanear producto
            if (productos.isEmpty()) {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onStockearClick("producto")
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color(0xFF1976D2), shape = RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Stockear",
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Escanear Partida",
                            color = Color.White,
                            fontSize = 28.sp
                        )
                    }
                }
            } else {
                // Mostrar lista de productos
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(8.dp)

                ) {
                    Text("Partidas")
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
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Eliminar producto",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }

                // Botón para agregar más productos
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Agregar Partida", color = Color.White)
                }
            }
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

            // Botón escanear ubicación o mostrar ubicación escaneada con refrescar
            if (ubicacion.isNullOrBlank()) {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onStockearClick("ubicacion")
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color(0xFF1976D2), shape = RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Stockear",
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Escanear ubicacion",
                            color = Color.White,
                            fontSize = 28.sp
                        )
                    }
                }
            } else {
                // Mostrar ubicación escaneada y botón refrescar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Text("ubicaciòn", modifier = Modifier.weight(1f))

                    Text(ubicacion ?: "-", modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        // Refrescar: volver a abrir la pantalla de escanear
                        if (hasCameraPermission) {
                            onStockearClick("ubicacion")
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refrescar ubicación",
                            tint = Color(0xFF1976D2)
                        )
                    }
                }
            }


           if(!productos.isNotEmpty() && !ubicacion.isNullOrBlank())
           {// Campo editable para observación
            OutlinedTextField(
                value = observacion,
                onValueChange = { observacion = it },
                label = { Text("Observación") },
                singleLine = false,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
           }
        }
        // Botón de enviar en la esquina inferior derecha
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            val enabled = productos.isNotEmpty() && !ubicacion.isNullOrBlank()
            var errorEnvio by remember { mutableStateOf<String?>(null) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Aquí el botón de enviar
                if (productos.isNotEmpty() && !ubicacion.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            errorEnvio = null
                            val partidas = productos.map { prod ->
                                EstibarPartida(
                                    nombreUbicacion = ubicacion ?: "",
                                    numPartida = prod
                                )
                            }
                            val fechaHora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
                            val usuario = prefs.getString("savedUser", "") ?: ""
                            val obsFinal = if (observacion.isNotBlank()) "$usuario: $observacion" else usuario
                            val request = EstibarPartidasRequest(
                                partidas = partidas,
                                fechaHora = fechaHora,
                                codDeposito = deposito,
                                observacion = obsFinal
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val response = ApiClient.apiService.estibarPartidas(request).execute()
                                    if (response.isSuccessful) {
                                        withContext(Dispatchers.Main) {
                                            context.startActivity(Intent(context, EstivacionSuccessActivity::class.java))
                                            (context as? Activity)?.finish()
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

@Preview(showBackground = true, name = "EstivacionScreen Preview")
@Composable
fun EstivacionScreenPreview() {
    EstivacionScreen(
        onBack = {},
        onStockearClick = {},
        producto = "123456789",
        ubicacion = "A-01",
    )
}
