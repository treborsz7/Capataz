import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.codegalaxy.barcodescanner.ui.theme.BarCodeScannerTheme

@Composable
fun EstivacionSuccessScreen(
    onFinish: () -> Unit
) {
    var showSuccess by remember { mutableStateOf(false) }

    // Pantalla de carga por 2 segundos
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showSuccess = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (!showSuccess) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1976D2))
                Spacer(modifier = Modifier.height(24.dp))
                Text("Guardando Exitoso...", color = Color.Black)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¡Estivación guardada correctamente!!",
                    color = Color(0xFF1976D2),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Finalizar", color = Color.White)
                }
            }
        }
    }
}
