import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codegalaxy.barcodescanner.model.RecolectarItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecolectarScreen(
    onBack: () -> Unit = {},
    onStockearClick: (boton: String) -> Unit = {},
    producto: String? = null,
    ubicacion: String? = null
) {
    val context = LocalContext.current
    var recolectarItems by remember { mutableStateOf(listOf<RecolectarItem>()) }
    var scanStep by remember { mutableStateOf("producto") }
    var productoActual by remember { mutableStateOf<String?>(null) }
    var ubicacionActual by remember { mutableStateOf<String?>(null) }
    var cantidadActual by remember { mutableStateOf("") }
    var unidadActual by remember { mutableStateOf("un") }
    val unidades = listOf("un", "kg", "lt")
    var expandedUnidad by remember { mutableStateOf(false) }
    var deposito by remember { mutableStateOf("") }
    var errorEnvio by remember { mutableStateOf<String?>(null) }

    // Manejo de escaneo
    LaunchedEffect(producto) {
        if (!producto.isNullOrBlank() && scanStep == "producto") {
            productoActual = producto
            scanStep = "ubicacion"
        }
    }
    LaunchedEffect(ubicacion) {
        if (!ubicacion.isNullOrBlank() && scanStep == "ubicacion") {
            ubicacionActual = ubicacion
            scanStep = "cantidad"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(WindowInsets.systemBars.asPaddingValues()),
        contentAlignment = Alignment.Center
    ) {
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
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
            // Proceso de escaneo y carga
            if (scanStep == "producto") {
                Button(
                    onClick = { onStockearClick("producto") },
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
                        Icon(Icons.Filled.Add, contentDescription = "Escanear producto", tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escanear producto", color = Color.White, fontSize = 18.sp)
                    }
                }
            } else if (scanStep == "ubicacion") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(productoActual ?: "-", modifier = Modifier.weight(1f))
                    IconButton(onClick = { onStockearClick("producto") }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Re-escanear producto", tint = Color(0xFF1976D2))
                    }
                }
                Button(
                    onClick = { onStockearClick("ubicacion") },
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
                        Icon(Icons.Filled.LocationOn, contentDescription = "Escanear ubicación", tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escanear ubicación", color = Color.White, fontSize = 18.sp)
                    }
                }
            } else if (scanStep == "cantidad") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(productoActual ?: "-", modifier = Modifier.weight(1f))
                    IconButton(onClick = { scanStep = "producto" }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Re-escanear producto", tint = Color(0xFF1976D2))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(ubicacionActual ?: "-", modifier = Modifier.weight(1f))
                    IconButton(onClick = { scanStep = "ubicacion" }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Re-escanear ubicación", tint = Color(0xFF1976D2))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    OutlinedTextField(
                        value = cantidadActual,
                        onValueChange = { cantidadActual = it.filter { c -> c.isDigit() } },
                        label = { Text("Cantidad") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        OutlinedTextField(
                            value = unidadActual,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad") },
                            modifier = Modifier.width(80.dp).clickable { expandedUnidad = true },
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
                        DropdownMenu(
                            expanded = expandedUnidad,
                            onDismissRequest = { expandedUnidad = false }
                        ) {
                            unidades.forEach { unidad ->
                                DropdownMenuItem(
                                    text = { Text(unidad) },
                                    onClick = {
                                        unidadActual = unidad
                                        expandedUnidad = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (!productoActual.isNullOrBlank() && !ubicacionActual.isNullOrBlank() && cantidadActual.isNotBlank()) {
                            recolectarItems = recolectarItems + RecolectarItem(
                                producto = productoActual!!,
                                ubicacion = ubicacionActual!!,
                                cantidad = cantidadActual,
                                unidad = unidadActual
                            )
                            productoActual = null
                            ubicacionActual = null
                            cantidadActual = ""
                            unidadActual = "un"
                            scanStep = "producto"
                        }
                    }) {
                        Text("Agregar")
                    }
                }
            }
            // Listado de productos recolectados
            if (recolectarItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    recolectarItems.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${item.producto} - ${item.ubicacion} - ${item.cantidad} ${item.unidad}", modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                recolectarItems = recolectarItems.filter { it != item }
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Eliminar", tint = Color.Red)
                            }
                        }
                    }
                }
            }
            // Botón enviar
            if (recolectarItems.isNotEmpty() && deposito.isNotBlank()) {
                Button(
                    onClick = {
                        errorEnvio = null
                        val prefs = context.getSharedPreferences("QRCodeScannerPrefs", Context.MODE_PRIVATE)
                        val usuario = prefs.getString("savedUser", "") ?: ""
                        // Build JSON body
                        val json = JSONObject()
                        json.put("usuario", usuario)
                        json.put("deposito", deposito)
                        val itemsArray = JSONArray()
                        recolectarItems.forEach { item ->
                            val itemObj = JSONObject()
                            itemObj.put("producto", item.producto)
                            itemObj.put("ubicacion", item.ubicacion)
                            itemObj.put("cantidad", item.cantidad)
                            itemObj.put("unidad", item.unidad)
                            itemsArray.put(itemObj)
                        }
                        json.put("items", itemsArray)
                        //val body = RequestBody.create("application/json", json.toString())
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val response = ApiClient.apiService.recolectarPedido().execute()
                                if (response.isSuccessful) {
                                    withContext(Dispatchers.Main) {
                                        recolectarItems = listOf()
                                        errorEnvio = null
                                        // Optionally show a success message or navigate
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
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
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


@Preview(showBackground = true, name = "RecolectarScreen Preview")
@Composable
fun RecolectarScreenPreview() {
    RecolectarScreen(
        onBack = {},
        onStockearClick = {},
        producto = "123456789",
        ubicacion = "A-01"
    )
}

