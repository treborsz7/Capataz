package com.thinkthat.mamusckascaner.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import com.thinkthat.mamusckascaner.model.OperationType
import androidx.compose.ui.tooling.preview.Preview
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import android.widget.Toast

@Composable
fun MainScreen(
    onScanRequest: (OperationType) -> Unit = {},
    onLogout: () -> Unit = {}
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
    
    // Estado para el botón de logs
    var showLogButton by remember { mutableStateOf(false) }
    var showSendLogDialog by remember { mutableStateOf(false) }
    
    // Verificar si existe el archivo de log
    LaunchedEffect(Unit) {
        val logFile = File(context.getExternalFilesDir(null), "logs/app_log.txt")
        showLogButton = logFile.exists() && logFile.length() > 0
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
        if (isGranted) onScanRequest(OperationType.ESTIVAR)
    }

    Box(
         modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(WindowInsets.systemBars.asPaddingValues()), // <-- Respeta la status bar
        contentAlignment = Alignment.Center
    ) {
        // Botón de logout en la esquina superior derecha
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                IconButton(
                    onClick = {
                        onLogout()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Logout,
                        contentDescription = "Logout",
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Botón de envío de logs (solo visible si hay logs)
                if (showLogButton) {
                    IconButton(
                        onClick = {
                            showSendLogDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = "Enviar logs",
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
        // Agrupar los botones en un Column centrado
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Botón Recolectar
            Button(
                onClick = {
                    val intent = android.content.Intent(context, RecolectarByQRActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory,
                        contentDescription = "Recolectar Pedido",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Recolectar Pedido",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }

            // Botón Estibar
            Button(
                onClick = {
                    if (hasCameraPermission) {
                        onScanRequest(OperationType.ESTIVAR)
                    } else {
                        launcher.launch(android.Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DashboardCustomize,
                        contentDescription = "Estibar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Estibar Partidas",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }

            // Botón Reubicar
            Button(
                onClick = {
                    val intent = android.content.Intent(context, ReubicacionActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = "Reubicar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Reubicar Partidas",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
    
    // Diálogo de confirmación para enviar logs
    if (showSendLogDialog) {
        AlertDialog(
            onDismissRequest = { showSendLogDialog = false },
            title = { Text("Enviar logs") },
            text = { Text("¿Desea enviar el log a soporte?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSendLogDialog = false
                        sendLogEmail(context)
                    }
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSendLogDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun sendLogEmail(context: android.content.Context) {
    try {
        // Obtener el archivo de log
        val logFile = File(context.getExternalFilesDir(null), "logs/app_log.txt")
        
        if (!logFile.exists() || logFile.length() == 0L) {
            Toast.makeText(context, "No hay logs para enviar", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Obtener información del usuario desde SharedPreferences
        val prefs = context.getSharedPreferences("QRCodeScannerPrefs", android.content.Context.MODE_PRIVATE)
        val username = prefs.getString("savedUser", "Usuario desconocido") ?: "Usuario desconocido"
        val empresa = prefs.getString("savedEmpresa", "Empresa desconocida") ?: "Empresa desconocida"
        
        // Crear URI del archivo usando FileProvider
        val logUri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Error al preparar el archivo: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }
        
        // Crear el intent de email con el archivo adjunto automáticamente
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"  // Tipo MIME para archivos de texto
            putExtra(Intent.EXTRA_EMAIL, arrayOf("robert-sz@hotmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Logs de aplicación - Usuario: $username")
            putExtra(Intent.EXTRA_TEXT, """
                Información del usuario:
                - Usuario: $username
                - Empresa: $empresa
                - Fecha de envío: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
                
                El archivo de logs se adjunta automáticamente a este correo.
            """.trimIndent())
            // Adjuntar el archivo de log automáticamente
            putExtra(Intent.EXTRA_STREAM, logUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            // Abrir el selector de aplicaciones de email con el archivo ya adjunto
            context.startActivity(Intent.createChooser(emailIntent, "Enviar logs por email"))
            
            // Nota: Eliminar el archivo después de abrir el selector
            // El archivo ya está disponible para el cliente de email
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (logFile.exists()) {
                    if (logFile.delete()) {
                        Toast.makeText(context, "Log preparado para envío y eliminado del dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }, 2000) // Esperar 2 segundos para asegurar que el archivo se haya adjuntado
            
        } catch (e: Exception) {
            Toast.makeText(context, "No hay aplicaciones de email disponibles", Toast.LENGTH_LONG).show()
        }
        
    } catch (e: Exception) {
        Toast.makeText(context, "Error al enviar logs: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true, name = "Main Screen - Height 800dp", heightDp = 800)
@Composable
fun MainScreenTallPreview() {
    BarCodeScannerTheme {
        MainScreen(
            onScanRequest = {},
            onLogout = {}
        )
    }
}
