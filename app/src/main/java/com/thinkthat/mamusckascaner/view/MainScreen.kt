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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalConfiguration
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
import com.thinkthat.mamusckascaner.utils.AppLogger

@Composable
fun MainScreen(
    onScanRequest: (OperationType) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // Responsive values
    val horizontalPadding = maxOf(minOf(screenWidth * 0.08f, 32.dp), 16.dp)
    val logoSize = maxOf(minOf(screenWidth * 0.7f, 450.dp), 300.dp)
    val buttonWidth = maxOf(minOf(screenWidth * 0.7f, 300.dp), 200.dp)
    val buttonHeight = maxOf(minOf(screenHeight * 0.08f, 70.dp), 50.dp)
    val buttonSpacing = maxOf(minOf(screenHeight * 0.02f, 20.dp), 12.dp)
    val buttonFontSize = maxOf(minOf((screenWidth * 0.045f).value, 20f), 16f).sp
    val topOffset = -maxOf(minOf(screenHeight * 0.05f, 50.dp), 30.dp)
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
    
    // Verificar si existen archivos de log de ERROR o EXCEPTION
    LaunchedEffect(Unit) {
        val errorLogFile = File(context.getExternalFilesDir(null), "logs/${AppLogger.LOG_FILE_ERROR}")
        val exceptionLogFile = File(context.getExternalFilesDir(null), "logs/${AppLogger.LOG_FILE_EXCEPTION}")
        showLogButton = (errorLogFile.exists() && errorLogFile.length() > 0) || 
                        (exceptionLogFile.exists() && exceptionLogFile.length() > 0)
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
            .background(Color(0xFFCD0914))
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
                        tint = Color.White,
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
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
        // Agrupar los botones en un Column centrado
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(buttonSpacing),
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .offset(y = topOffset) // Mover todo el contenido hacia arriba
        ) {
            // Icono de la app centrado
            Icon(
                painter = painterResource(id = com.thinkthat.mamusckascaner.R.drawable.logos_y__1__05__1_),
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(logoSize)

            )

            // Botón Recolectar
            Button(
                onClick = {
                    val intent = android.content.Intent(context, RecolectarByQRActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
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
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Recolectar Pedido",
                        color = Color.Black,
                        fontSize = buttonFontSize
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
                    .height(buttonHeight),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
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
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Estibar Partidas",
                        color = Color.Black,
                        fontSize = buttonFontSize
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
                    .height(buttonHeight),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
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
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Reubicar Partidas",
                        color = Color.Black,
                        fontSize = buttonFontSize
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
        // Obtener todos los archivos de log
        val infoLogFile = File(context.getExternalFilesDir(null), "logs/${AppLogger.LOG_FILE_INFO}")
        val errorLogFile = File(context.getExternalFilesDir(null), "logs/${AppLogger.LOG_FILE_ERROR}")
        val exceptionLogFile = File(context.getExternalFilesDir(null), "logs/${AppLogger.LOG_FILE_EXCEPTION}")
        
        // Recopilar archivos que existen y tienen contenido
        val logFiles = mutableListOf<File>()
        val logUris = ArrayList<Uri>()
        
        if (infoLogFile.exists() && infoLogFile.length() > 0) {
            logFiles.add(infoLogFile)
        }
        if (errorLogFile.exists() && errorLogFile.length() > 0) {
            logFiles.add(errorLogFile)
        }
        if (exceptionLogFile.exists() && exceptionLogFile.length() > 0) {
            logFiles.add(exceptionLogFile)
        }
        
        if (logFiles.isEmpty()) {
            Toast.makeText(context, "No hay logs para enviar", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Crear URIs de los archivos usando FileProvider
        try {
            logFiles.forEach { file ->
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                logUris.add(uri)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al preparar los archivos: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }
        
        // Obtener información del usuario desde SharedPreferences
        val prefs = context.getSharedPreferences("QRCodeScannerPrefs", android.content.Context.MODE_PRIVATE)
        val username = prefs.getString("savedUser", "Usuario desconocido") ?: "Usuario desconocido"
        val empresa = prefs.getString("savedEmpresa", "Empresa desconocida") ?: "Empresa desconocida"
        
        // Crear el intent de email con múltiples archivos adjuntos
        val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/plain"  // Tipo MIME para archivos de texto
            putExtra(Intent.EXTRA_EMAIL, arrayOf("robert-sz@hotmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Logs de aplicación - Usuario: $username")
            putExtra(Intent.EXTRA_TEXT, """
                Información del usuario:
                - Usuario: $username
                - Empresa: $empresa
                - Fecha de envío: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
                
                Se adjuntan ${logFiles.size} archivo(s) de logs automáticamente a este correo.
            """.trimIndent())
            // Adjuntar todos los archivos de log
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, logUris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            // Abrir el selector de aplicaciones de email con los archivos ya adjuntos
            context.startActivity(Intent.createChooser(emailIntent, "Enviar logs por email"))
            
            // Nota: Los archivos NO se eliminan automáticamente para asegurar que el cliente de email
            // pueda leer los adjuntos correctamente. El usuario puede eliminarlos manualmente si lo desea,
            // o se sobrescribirán en la próxima sesión de la aplicación.
            
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
