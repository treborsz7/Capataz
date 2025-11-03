import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.sp
@Composable
fun EstivacionSuccessScreen(
    onFinish: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // Responsive values
    val spacing = maxOf(minOf(screenHeight * 0.03f, 32.dp), 16.dp)
    val buttonWidth = maxOf(minOf(screenWidth * 0.5f, 200.dp), 120.dp)
    val titleFontSize = maxOf(minOf((screenWidth * 0.05f).value, 24f), 18f).sp
    val bodyFontSize = maxOf(minOf((screenWidth * 0.04f).value, 18f), 14f).sp
    
    var showSuccess by remember { mutableStateOf(false) }

    // Pantalla de carga por 2 segundos
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showSuccess = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCD0914)),
        contentAlignment = Alignment.Center
    ) {
        if (!showSuccess) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(spacing))
                Text("Guardando Exitoso...", color = Color.White, fontSize = bodyFontSize)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¡Estivación guardada correctamente!!",
                    color = Color.White,
                    fontSize = titleFontSize
                )
                Spacer(modifier = Modifier.height(spacing))
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.width(buttonWidth)
                ) {
                    Text("Finalizar", color = Color.Black, fontSize = bodyFontSize)
                }
            }
        }
    }
}
