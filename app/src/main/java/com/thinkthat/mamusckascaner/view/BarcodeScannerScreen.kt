import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.platform.LocalConfiguration

import com.thinkthat.mamusckascaner.BarScanState
import com.thinkthat.mamusckascaner.model.BarCodeAnalyzer
import com.thinkthat.mamusckascaner.viewmodel.BarCodeScannerViewModel
import com.thinkthat.mamusckascaner.utils.AppLogger

@Composable
fun BarcodeScannerScreen(
    viewModel: BarCodeScannerViewModel,
    onBack: () -> Unit = {},
    onScanResult: (String) -> Unit = {},
    scannerKey: Int,
) {
    LaunchedEffect(scannerKey) {
        viewModel.resetState()
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // Responsive values
    val horizontalPadding = maxOf(minOf(screenWidth * 0.08f, 32.dp), 16.dp)
    val buttonHeight = maxOf(minOf(screenHeight * 0.07f, 64.dp), 48.dp)
    val bodyFontSize = maxOf(minOf((screenWidth * 0.04f).value, 18f), 14f).sp
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
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Estructura principal: columna con botón arriba y cámara debajo
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCD0914))
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        // Botón de volver arriba a la izquierda
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = horizontalPadding / 2, top = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
        }
        // Cámara y contenido debajo del botón de back
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Ocupa el resto de la pantalla
        ) {
            if (hasCameraPermission) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CameraPreview(
                        viewModel = viewModel,
                        onScanResult = onScanResult,
                        onBack = onBack,
                        horizontalPadding = horizontalPadding,
                        buttonHeight = buttonHeight,
                        bodyFontSize = bodyFontSize
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Camera permission is required for scanning barcodes", color = Color.White, fontSize = bodyFontSize)
                    Button(
                        onClick = { launcher.launch(android.Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.height(buttonHeight)
                    ) {
                        Text("Grant Permission", color = Color.Black, fontSize = bodyFontSize)
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    viewModel: BarCodeScannerViewModel,
    onScanResult: (String) -> Unit = {},
    onBack: () -> Unit = {},
    horizontalPadding: Dp = 16.dp,
    buttonHeight: Dp = 56.dp,
    bodyFontSize: TextUnit = 16.sp
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var preview by remember { mutableStateOf<Preview?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val barScanState = viewModel.barScanState
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Detener la cámara solo cuando se detecta un código
    LaunchedEffect(barScanState) {
        if (barScanState is BarScanState.ScanSuccess) {
            cameraProvider?.unbindAll()
            // Quitar: onScanResult(value)
        }
    }

    // Liberar recursos al salir del Composable
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // Mantener la cámara activa hasta detectar un código
            if (barScanState !is BarScanState.ScanSuccess) {
                AndroidView(
                    factory = { androidViewContext ->
                        PreviewView(androidViewContext).apply {
                            this.scaleType = PreviewView.ScaleType.FILL_CENTER
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f),
                    update = { previewView ->
                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()
                        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                            ProcessCameraProvider.getInstance(context)

                        cameraProviderFuture.addListener({
                            preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val provider: ProcessCameraProvider = cameraProviderFuture.get()
                            cameraProvider = provider
                            val barcodeAnalyzer = BarCodeAnalyzer(viewModel)
                            val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor, barcodeAnalyzer)
                                }

                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                AppLogger.logError(
                                    tag = "BarcodeScannerScreen",
                                    message = "Error al iniciar cámara: ${e.message}",
                                    throwable = e
                                )
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )
            }
        }

        when (barScanState) {
            is BarScanState.Ideal -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Position the barcode in front of the camera.", color = Color.White)
                }
            }

            is BarScanState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scanning...", color = Color.White)
                }
            }
            is BarScanState.ScanSuccess -> {
                // Limpiar el valor escaneado usando el mismo regex que en la Activity
                val raw = barScanState.rawValue ?: "Sin valor"
                val cleaned = raw
                    .replace(Regex("\\s+"), " ")
                    .replace(Regex("\\]?C1", RegexOption.IGNORE_CASE), "")
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    
                ) {
                    Text(
                        text = "Valor escaneado:",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cleaned,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { onScanResult(cleaned) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Usar este valor", color = Color.Black)
                        }
                        Button(
                            onClick = { viewModel.resetState() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Escanear nuevamente", color = Color.Black)
                        }
                    }
                }
            }
            is BarScanState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = barScanState.error,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onBack() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("Volver", color = Color.Black)
                    }
                }
            }
        }
    }
}

