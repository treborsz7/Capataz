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
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues


@Composable
fun MainScreen(
    onStockearClick: () -> Unit = {},
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
        if (isGranted) onStockearClick()
    }

    Box(
         modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()), // <-- Respeta la status bar
        contentAlignment = Alignment.Center
    ) {
        // Botón de logout en la esquina superior derecha
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = {
                    // Eliminar recordar del store y volver a login
                    val prefs = context.getSharedPreferences("QRCodeScannerPrefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().remove("savedUser").remove("savedPass").putBoolean("savedRemember", false).apply()
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
        Button(
            onClick = {
                if (hasCameraPermission) {
                    onStockearClick()
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DashboardCustomize,
                    contentDescription = "Estibar",
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Estibar",
                    color = Color.White,
                    fontSize = 28.sp
                )
            }
        }
    }
}
