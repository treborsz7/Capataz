package com.thinkthat.mamusckascaner.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thinkthat.mamusckascaner.di.ViewModelFactories
import com.thinkthat.mamusckascaner.presentation.recolectar.RecolectarViewModel
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme
import com.thinkthat.mamusckascaner.domain.model.parsearQr

/**
 * Contenedor Android de la recolección: resuelve el QR de entrada, el escáner y
 * la navegación. La lógica vive en [RecolectarViewModel].
 */
class RecolectarActivity : ComponentActivity() {

    private val viewModel: RecolectarViewModel by viewModels { ViewModelFactories.recolectar }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val qrCrudo = intent.getStringExtra(EXTRA_QR_DATA)
        val vieneDeQR = intent.getBooleanExtra(EXTRA_FROM_QR, false)
        val qrData = if (vieneDeQR && qrCrudo != null) parsearQr(qrCrudo) else null

        val idPedido = qrData?.pedido?.toIntOrNull() ?: intent.getIntExtra(EXTRA_ORDEN_ID, -1)
        viewModel.iniciar(idPedido = idPedido, qrDeposito = qrData?.deposito)

        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.state.collectAsState()

                    // Qué renglón está esperando un escaneo: (artículo, índice, campo).
                    var objetivoEscaneo by rememberSaveable {
                        mutableStateOf<Triple<String, Int, String>?>(null)
                    }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        val objetivo = objetivoEscaneo
                        val valor = result.data?.getStringExtra("scanResult")
                        if (result.resultCode == Activity.RESULT_OK &&
                            objetivo != null &&
                            !valor.isNullOrBlank()
                        ) {
                            val (codArticulo, indice, campo) = objetivo
                            when (campo) {
                                CAMPO_PARTIDA ->
                                    viewModel.onPartidaEscaneada(codArticulo, indice, valor)
                                CAMPO_UBICACION ->
                                    viewModel.onUbicacionEscaneada(codArticulo, indice, valor)
                            }
                        }
                        objetivoEscaneo = null
                    }

                    RecolectarScreen(
                        state = state,
                        onBack = { finish() },
                        onClose = { volverAlMenu() },
                        onEscanear = { codArticulo, indice, campo ->
                            objetivoEscaneo = Triple(codArticulo, indice, campo)
                            scannerLauncher.launch(
                                Intent(this, BarcodeScannerActivity::class.java)
                                    .putExtra("modo", campo)
                            )
                        },
                        onCantidadChange = viewModel::onCantidadChange,
                        onAsegurarEscaneos = viewModel::asegurarEscaneos,
                        onGuardarRenglon = viewModel::guardarRenglon,
                        onEditarCantidad = viewModel::onEditarCantidad,
                        onEliminarRenglon = viewModel::eliminarRenglon,
                        onRetryUbicaciones = viewModel::cargarUbicaciones,
                        onEnviar = viewModel::enviar,
                        onDismissError = viewModel::limpiarError,
                        onSuccess = { volverAlMenu() }
                    )
                }
            }
        }
    }

    private fun volverAlMenu() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }

    companion object {
        const val EXTRA_QR_DATA = "qrData"
        const val EXTRA_FROM_QR = "fromQR"
        const val EXTRA_ORDEN_ID = "ordenId"

        const val CAMPO_PARTIDA = "partida"
        const val CAMPO_UBICACION = "ubicacion"
    }
}
