package com.thinkthat.mamusckascaner.view

import ReubicacionScreen
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thinkthat.mamusckascaner.di.ViewModelFactories
import com.thinkthat.mamusckascaner.presentation.reubicacion.CampoReubicacion
import com.thinkthat.mamusckascaner.presentation.reubicacion.ReubicacionEvent
import com.thinkthat.mamusckascaner.presentation.reubicacion.ReubicacionListViewModel
import com.thinkthat.mamusckascaner.presentation.reubicacion.ReubicacionPantalla
import com.thinkthat.mamusckascaner.presentation.reubicacion.ReubicacionViewModel
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme

/**
 * Contenedor Android del flujo de reubicación: Intents, escáner y navegación.
 * La lógica vive en [ReubicacionViewModel].
 */
class ReubicacionActivity : ComponentActivity() {

    private val viewModel: ReubicacionViewModel by viewModels { ViewModelFactories.reubicacion }
    private val listViewModel: ReubicacionListViewModel by viewModels {
        ViewModelFactories.reubicacionList
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.iniciar(
            retomar = intent.getBooleanExtra(EXTRA_RETOMAR, false),
            id = intent.getLongExtra(EXTRA_ID, -1L),
            partida = intent.getStringExtra(EXTRA_PARTIDA),
            ubicacionOrigen = intent.getStringExtra(EXTRA_UBICACION_ORIGEN),
            ubicacionDestino = intent.getStringExtra(EXTRA_UBICACION_DESTINO)
        )

        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.state.collectAsState()
                    val listState by listViewModel.state.collectAsState()

                    var campoEscaneado by rememberSaveable { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        viewModel.events.collect { evento ->
                            when (evento) {
                                ReubicacionEvent.EnvioExitoso -> {
                                    startActivity(
                                        Intent(
                                            this@ReubicacionActivity,
                                            EstivacionSuccessActivity::class.java
                                        )
                                    )
                                    finish()
                                }
                            }
                        }
                    }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            viewModel.onScanResult(
                                campo = CampoReubicacion.desdeClave(campoEscaneado),
                                valor = result.data?.getStringExtra("scanResult")
                            )
                        }
                    }

                    when (state.pantalla) {
                        ReubicacionPantalla.CARGANDO -> Unit

                        ReubicacionPantalla.LISTA_PENDIENTES -> ReubicacionListScreen(
                            state = listState,
                            onBack = { finish() },
                            onNewReubicacion = { viewModel.nuevaReubicacion() },
                            onResumeReubicacion = { viewModel.retomarReubicacion(it) },
                            onDeleteReubicacion = { listViewModel.eliminar(it) }
                        )

                        ReubicacionPantalla.FORMULARIO -> ReubicacionScreen(
                            state = state,
                            onBack = { finish() },
                            onReubicarClick = { tipo ->
                                campoEscaneado = tipo
                                scannerLauncher.launch(
                                    Intent(this, BarcodeScannerActivity::class.java)
                                        .putExtra("modo", tipo)
                                )
                            },
                            onProductoChange = viewModel::onPartidaChange,
                            onUbicacionOrigenChange = viewModel::onUbicacionOrigenChange,
                            onUbicacionDestinoChange = viewModel::onUbicacionDestinoChange,
                            onEnviar = viewModel::enviar,
                            onDismissError = viewModel::limpiarError
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_RETOMAR = "retomar"
        const val EXTRA_PARTIDA = "partida"
        const val EXTRA_UBICACION_ORIGEN = "ubicacionOrigen"
        const val EXTRA_UBICACION_DESTINO = "ubicacionDestino"
        const val EXTRA_ID = "id"
    }
}
