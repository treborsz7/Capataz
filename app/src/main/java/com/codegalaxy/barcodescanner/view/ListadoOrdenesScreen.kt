package com.codegalaxy.barcodescanner.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import com.codegalaxy.barcodescanner.model.OrdenTrabajo
import com.codegalaxy.barcodescanner.model.ItemOrden

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoOrdenesScreen(
    onBack: () -> Unit = {},
    onTomaOrden: (OrdenTrabajo) -> Unit = {}
) {
    // Ejemplo de datos mockeados
    val ordenes = remember {
        listOf(
            OrdenTrabajo(
                nroorden = 1,
                listadoitems = listOf(
                    ItemOrden(nropartida = "P001", ubicacion = "A-01", cantidad = 10),
                    ItemOrden(nropartida = "P002", ubicacion = "A-02", cantidad = 5)
                ),
                fechahora = "2025-07-06T10:00:00Z",
                estado = "Pendiente"
            ),
            OrdenTrabajo(
                nroorden = 2,
                listadoitems = listOf(
                    ItemOrden(nropartida = "P003", ubicacion = "B-01", cantidad = 7)
                ),
                fechahora = "2025-07-06T11:00:00Z",
                estado = "Pendiente"
            )
        )
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.95f)
        ) {
            Text(
                text = "Listado de Órdenes",
                color = Color(0xFF1976D2),
                fontSize = 24.sp,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Orden N°: ${orden.nroorden}",
                                    color = Color.Black,
                                    fontSize = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { onTomaOrden(orden) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Toma", color = Color.White, fontSize = 14.sp)
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
