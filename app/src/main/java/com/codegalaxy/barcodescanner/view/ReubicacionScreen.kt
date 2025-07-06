import android.app.Activity
import android.content.Intent
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
import com.codegalaxy.barcodescanner.view.EstivacionSuccessActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
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
    LaunchedEffect(deposito) {
        prefs.edit().putString("savedDeposito", deposito).apply()
    }
    var productoActual by remember { mutableStateOf<String?>(null) }
    var ubicacionOrigenActual by remember { mutableStateOf<String?>(null) }
    var ubicacionDestinoActual by remember { mutableStateOf<String?>(null) }
    var errorEnvio by remember { mutableStateOf<String?>(null) }
    var observacion:String = ""
    // Manejo de escaneo
    LaunchedEffect(producto) {
        if (!producto.isNullOrBlank()) {
            productoActual = producto
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
            // Botón escanear producto o mostrar producto escaneado
            if (productoActual.isNullOrBlank()) {
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
                            contentDescription = "Escanear producto",
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Text(productoActual ?: "-", modifier = Modifier.weight(1f), color=Color.Black)
                    IconButton(onClick = {
                        if (hasCameraPermission) {
                            onReubicarClick("producto")
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refrescar producto",
                            tint = Color(0xFF1976D2)
                        )
                    }
                }
            }
            // Botón escanear ubicación origen o mostrar ubicación origen escaneada
            if (!productoActual.isNullOrBlank() && ubicacionOrigenActual.isNullOrBlank()) {
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
            } else if (!productoActual.isNullOrBlank() && !ubicacionOrigenActual.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Text(ubicacionOrigenActual ?: "-", modifier = Modifier.weight(1f), color=Color.Black)
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
            // Botón escanear ubicación destino o mostrar ubicación destino escaneada
            if (!productoActual.isNullOrBlank() && !ubicacionOrigenActual.isNullOrBlank() && ubicacionDestinoActual.isNullOrBlank()) {
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
            } else if (!productoActual.isNullOrBlank() && !ubicacionOrigenActual.isNullOrBlank() && !ubicacionDestinoActual.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Text(ubicacionDestinoActual ?: "-", modifier = Modifier.weight(1f), color=Color.Black)
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
            // Campo observación
            OutlinedTextField(
                value = observacion,
                onValueChange = { observacion = it },
                label = { Text("Observación") },
                singleLine = false,
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
            // Botón de enviar solo si los tres campos están cargados
            if (!productoActual.isNullOrBlank() && !ubicacionOrigenActual.isNullOrBlank() && !ubicacionDestinoActual.isNullOrBlank() && deposito.isNotBlank()) {
                Button(
                    onClick = {
                        errorEnvio = null
                        val fechaHora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(
                            Date()
                        )

                        val usuario = prefs.getString("savedUser", "") ?: ""
                        val obsFinal = if (observacion.isNotBlank()) "$usuario: $observacion" else usuario

                        val reubicacion = ReubicarPartida(
                                nombreUbiOrigen = ubicacionOrigenActual ?: "",
                                nombreUbiDestino= ubicacionDestinoActual ?: "",
                                numPartida = productoActual!!
                            )


                        val request = ReubicarPartidasRequest(
                            reubicaciones = mutableListOf(reubicacion),
                            fechaHora = fechaHora,
                            codDeposito = deposito,
                            observacion = obsFinal,
                            reubicacion = 0
                        )

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val response = ApiClient.apiService.reubicarPartidas(request).execute()
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
