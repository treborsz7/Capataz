package com.codegalaxy.barcodescanner.view

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.platform.LocalContext
import com.thinkthat.mamusckascaner.service.Services.ApiClient
import com.thinkthat.mamusckascaner.service.Services.OrdenLanzada
import com.thinkthat.mamusckascaner.view.components.ErrorMessage
import com.thinkthat.mamusckascaner.view.components.LoadingMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoOrdenesScreen(
    onBack: () -> Unit = {},
    onTomaOrden: (OrdenLanzada) -> Unit = {}
) {
    val context = LocalContext.current
    var ordenes by remember { mutableStateOf<List<OrdenLanzada>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Función para cargar las órdenes desde la API
    fun cargarOrdenes() {
        isLoading = true
        errorMessage = null
        
        ApiClient.apiService.obtenerOrdenesLanzadas().enqueue(object : Callback<List<OrdenLanzada>> {
            override fun onResponse(call: Call<List<OrdenLanzada>>, response: Response<List<OrdenLanzada>>) {
                isLoading = false
                 if (response.isSuccessful) {
                    ordenes = response.body() ?: emptyList()
                } else {
                    errorMessage = "Error al cargar órdenes: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<OrdenLanzada>>, t: Throwable) {
                isLoading = false
                errorMessage = "Error de conexión: ${t.message}"
            }
        })
    }

    // Cargar órdenes al inicializar la pantalla
    LaunchedEffect(Unit) {
        cargarOrdenes()
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
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color(0xFF1976D2))
            }
        }
        
        // Botón de refrescar en la esquina superior derecha
        if(!ordenes.isEmpty() && !isLoading)
            Box(

            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
            ) {
                IconButton(onClick = { cargarOrdenes() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refrescar", tint = Color(0xFF1976D2))
                }
            }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.95f)
                .padding(top = 60.dp)
        ) {
            Text(
                text = "Listado de Órdenes Lanzadas",
                color = Color(0xFF1976D2),
                fontSize = 24.sp,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            when {
                isLoading -> {
                    LoadingMessage(
                        message = "Cargando órdenes...",
                        modifier = Modifier.padding(32.dp)
                    )
                }
                
                errorMessage != null -> {
                    ErrorMessage(
                        message = errorMessage!!,
                        onRetry = { cargarOrdenes() },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }
                
                ordenes.isEmpty() -> {
                    Text(
                        text = "No hay órdenes disponibles",
                        color = Color.Gray,
                        modifier = Modifier.padding(32.dp)
                    )
                }
                
                else -> {
                    // Listado scrolleable
                    Box(
                        modifier = Modifier.weight(1f, fill = false).fillMaxWidth()
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(ordenes.size) { idx ->
                                val orden = ordenes[idx]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Orden N°: ${orden.numero}",
                                                    color = Color.Black,
                                                    fontSize = 18.sp,
                                                    style = MaterialTheme.typography.titleMedium
                                                )

                                                Text(
                                                    text = "Producto: ${orden.producto.descripcion}",
                                                    color = Color.Gray,
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                                if (orden.unificadora) {
                                                    Text(
                                                        text = "✓ Unificadora",
                                                        color = Color.Green,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                            Button(
                                                enabled = true,
                                                onClick = { onTomaOrden(orden) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text("Tomar", color = Color.White, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListadoOrdenesScreenPreview() {
    ListadoOrdenesScreen()
}
