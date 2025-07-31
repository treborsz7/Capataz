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
            IconButton(
                onClick = {
                    onLogout()
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Logout,
                    contentDescription = "Logout",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        // Agrupar los botones en un Column centrado
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

               Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    enabled=true,
                    onClick = {
                        onScanRequest(OperationType.RECOLECTAR)
                    },
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color(0xFF1976D2), shape = RoundedCornerShape(24.dp))
                        .padding(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory,
                            contentDescription = "Recolectar Pedido",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recolectar",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

             Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            onScanRequest(OperationType.ESTIVAR)
                        } else {
                            launcher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .size(180.dp)
                        .background(Color(0xFF1976D2), shape = RoundedCornerShape(24.dp))
                        .padding(8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DashboardCustomize,
                            contentDescription = "Estibar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Estibar",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            
            Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        val intent = android.content.Intent(context, ReubicacionActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color(0xFF1976D2), shape = RoundedCornerShape(18.dp))
                        .padding(0.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = "Reubicar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reubicar",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
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
