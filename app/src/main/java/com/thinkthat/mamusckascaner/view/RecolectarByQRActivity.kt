package com.thinkthat.mamusckascaner.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thinkthat.mamusckascaner.di.ViewModelFactories
import com.thinkthat.mamusckascaner.domain.model.DatosQr
import com.thinkthat.mamusckascaner.domain.model.aContenidoQr
import com.thinkthat.mamusckascaner.presentation.recolectar.RecolectarQrViewModel
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme

/**
 * Paso previo a la recolección: escanear el QR del pedido o retomar uno pendiente.
 * La validación del QR y la lista de pendientes viven en [RecolectarQrViewModel].
 */
class RecolectarByQRActivity : ComponentActivity() {

    private val viewModel: RecolectarQrViewModel by viewModels { ViewModelFactories.recolectarQr }

    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onQrEscaneado(result.data?.getStringExtra("scanResult"))
        }
    }

    private val recolectarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Al volver de la recolección se refresca la lista de pendientes.
        viewModel.cargar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.state.collectAsState()

                    // Un QR válido abre la recolección; el ViewModel lo da por consumido.
                    LaunchedEffect(state.qrAceptado) {
                        state.qrAceptado?.let { contenido ->
                            abrirRecoleccion(contenido)
                            viewModel.qrConsumido()
                        }
                    }

                    RecolectarByQRScreen(
                        state = state,
                        onBack = { finish() },
                        onScanQR = {
                            qrScannerLauncher.launch(
                                Intent(this, BarcodeScannerActivity::class.java)
                                    .putExtra("modo", "orden")
                            )
                        },
                        onDeletePedido = viewModel::eliminar,
                        onResumePedido = { pedido ->
                            abrirRecoleccion(
                                DatosQr(
                                    deposito = pedido.codDeposito,
                                    pedido = pedido.idPedido.toString()
                                ).aContenidoQr()
                            )
                        }
                    )
                }
            }
        }
    }

    private fun abrirRecoleccion(contenidoQr: String) {
        recolectarLauncher.launch(
            Intent(this, RecolectarActivity::class.java)
                .putExtra(RecolectarActivity.EXTRA_QR_DATA, contenidoQr)
                .putExtra(RecolectarActivity.EXTRA_FROM_QR, true)
        )
    }
}
