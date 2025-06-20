import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstivacionScreen(
    onBack: () -> Unit = {},
    onStockearClick: (boton: String) -> Unit = {},
    producto: String?,
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
    // Guardar depósito cada vez que cambia
    LaunchedEffect(deposito) {
        prefs.edit().putString("savedDeposito", deposito).apply()
    }
    // Estado para ubicaciones
    var ubicaciones by remember { mutableStateOf(listOf<UbicacionResponse>()) }
    var ubicacionSeleccionada by remember { mutableStateOf<String?>(null) }
    var expandedUbicaciones by remember { mutableStateOf(false) }

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
            // Botón escanear producto
            Button(
                onClick = {
                    if (hasCameraPermission) {
                        onStockearClick("producto")
                        // Llamar a UbicacionesParaEstibar tras escanear producto
                        if (!producto.isNullOrBlank() && deposito.isNotBlank()) {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val response = ApiClient.apiService.ubicacionesParaEstibar(
                                        //idEmp = prefs.getString("savedEmpresa", "")?.toIntOrNull() ?: 0,
                                        codDeposi = deposito,
                                        codArticu = producto,
                                        optimizaRecorrido = false
                                    ).execute()
                                    if (response.isSuccessful && response.body() != null) {
                                        val json = response.body()!!.string()
                                        val gson = Gson()
                                        val type = object : TypeToken<List<UbicacionResponse>>() {}.type
                                        val ubicacionesList: List<UbicacionResponse> = gson.fromJson(json, type)
                                        withContext(Dispatchers.Main) {
                                            ubicaciones = ubicacionesList
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Manejo de error opcional
                                }
                            }
                        }
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
                        text = "Escanear producto",
                        color = Color.White,
                        fontSize = 28.sp
                    )
                }
            }
            // Dropdown de ubicaciones
            if (ubicaciones.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expandedUbicaciones,
                    onExpandedChange = { expandedUbicaciones = !expandedUbicaciones },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    OutlinedTextField(
                        value = ubicaciones.find { it.numero.toString() == ubicacionSeleccionada }?.nombre ?: "Selecciona ubicación",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ubicación disponible") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUbicaciones) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedUbicaciones,
                        onDismissRequest = { expandedUbicaciones = false }
                    ) {
                        ubicaciones.forEach {(nombre, numero) ->
                            DropdownMenuItem(
                                text = { nombre },
                                onClick = {
                                    ubicacionSeleccionada = numero.toString()
                                    expandedUbicaciones = false
                                }
                            )
                        }
                    }
                }
            }
            // Botón escanear ubicación
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
            // Aquí debajo de los botones
            Column(Modifier.padding(16.dp)) {
                Text("Producto: ${producto ?: "-"}")
                Text("Ubicación: ${ubicacion ?: "-"}")
            }
        }
        // Botón de enviar en la esquina inferior derecha
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            val enabled = !producto.isNullOrBlank() && !ubicacion.isNullOrBlank()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        context.startActivity(Intent(context, EstivacionSuccessActivity::class.java))
                        // Si quieres cerrar la activity actual:
                        (context as? Activity)?.finish()
                    },
                    enabled = enabled,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (enabled) Color(0xFF1976D2) else Color.Gray
                    ),
                    modifier = Modifier.size(64.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enviar",
                    color = if (enabled) Color(0xFF1976D2) else Color.Gray,
                    fontSize = 14.sp
                )
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
