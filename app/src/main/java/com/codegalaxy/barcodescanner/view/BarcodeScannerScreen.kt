import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.codegalaxy.barcodescanner.model.BarCodeAnalyzer
import com.codegalaxy.barcodescanner.viewmodel.BarCodeScannerViewModel
import com.codegalaxy.barcodescanner.BarScanState
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner


@Composable
fun BarcodeScannerScreen(
    viewModel: BarCodeScannerViewModel
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
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(android.Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        CameraPreview(viewModel)
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Camera permission is required for scanning barcodes")
            Button(onClick = { launcher.launch(android.Manifest.permission.CAMERA) }) {
                Text("Grant Permission")
            }
        }
    }
}


@Composable
fun CameraPreview(viewModel: BarCodeScannerViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var preview by remember { mutableStateOf<Preview?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val barScanState = viewModel.barScanState

    // Effect to unbind camera use cases when scan is successful
    LaunchedEffect(barScanState) {
        if (barScanState is BarScanState.ScanSuccess) {
            cameraProvider?.unbindAll()
        }
    }

    Column {
        Box(
            modifier = Modifier
                .size(400.dp)
                .padding(16.dp)
        ) {
            if (barScanState !is BarScanState.ScanSuccess) {
                AndroidView(
                    factory = { androidViewContext ->
                        PreviewView(androidViewContext).apply {
                            this.scaleType = PreviewView.ScaleType.FILL_START
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { previewView ->
                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()
                        val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
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
                                Log.d("CameraPreview", "Error: ${e.localizedMessage}")
                            }

                            lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                                override fun onDestroy(owner: LifecycleOwner) {
                                    cameraExecutor.shutdown()
                                }
                            })
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
                    Text("Position the barcode in front of the camera.")
                }
            }
            is BarScanState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scanning...")
                }
            }
            is BarScanState.ScanSuccess -> {
                if (barScanState.barStateModel != null) {
                    // JSON QR Code result
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Invoice Id: ${barScanState.barStateModel.invoiceNumber}")
                        Text("Name: ${barScanState.barStateModel.client.name}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Purchases:", style = MaterialTheme.typography.titleMedium)
                        barScanState.barStateModel.purchase.forEach { item ->
                            Text("${item.item}: ${item.quantity} x $${item.price}")
                        }
                        Text("Total Amount: $${barScanState.barStateModel.totalAmount}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetState() }) {
                            Text("Scan Another")
                        }
                    }
                } else {
                    // Regular barcode result
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Format: ${barScanState.format}",
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Value: ${barScanState.rawValue}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetState() }) {
                            Text("Scan Another")
                        }
                    }
                }
            }
            is BarScanState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error: ${barScanState.error}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.resetState() }) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@Composable
fun ScanResultContent(scanSuccess: BarScanState.ScanSuccess, onRescan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (scanSuccess.barStateModel != null) {
            // Display JSON content
            Text("Invoice Id: ${scanSuccess.barStateModel.invoiceNumber}")
            Text("Name: ${scanSuccess.barStateModel.client.name}")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Purchases:", style = MaterialTheme.typography.titleMedium)
            scanSuccess.barStateModel.purchase.forEach { item ->
                Text("${item.item}: ${item.quantity} x $${item.price}")
            }
            Text("Total Amount: $${scanSuccess.barStateModel.totalAmount}")
        } else {
            // Display raw barcode content
            Text("Format: ${scanSuccess.format}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Value: ${scanSuccess.rawValue}")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRescan) {
            Text("Scan Another")
        }
    }
}