package com.thinkthat.mamusckascaner.view

import EstivacionScreen
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.thinkthat.mamusckascaner.di.ViewModelFactories
import com.thinkthat.mamusckascaner.presentation.estivacion.CampoEstivacion
import com.thinkthat.mamusckascaner.presentation.estivacion.EstivacionEvent
import com.thinkthat.mamusckascaner.presentation.estivacion.EstivacionListViewModel
import com.thinkthat.mamusckascaner.presentation.estivacion.EstivacionPantalla
import com.thinkthat.mamusckascaner.presentation.estivacion.EstivacionViewModel
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme

/**
 * Contenedor Android del flujo de estivación. Solo resuelve lo que es propio de
 * la plataforma: Intents, escáner y navegación. La lógica está en
 * [EstivacionViewModel], que se reutiliza tal cual en iOS.
 */
class EstivacionActivity : ComponentActivity() {

    private val viewModel: EstivacionViewModel by viewModels { ViewModelFactories.estivacion }
    private val listViewModel: EstivacionListViewModel by viewModels {
        ViewModelFactories.estivacionList
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.iniciar(
            retomar = intent.getBooleanExtra(EXTRA_RETOMAR, false),
            id = intent.getLongExtra(EXTRA_ID, -1L),
            partida = intent.getStringExtra(EXTRA_PARTIDA),
            ubicacion = intent.getStringExtra(EXTRA_UBICACION)
        )

        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    val state by viewModel.state.collectAsState()
                    val listState by listViewModel.state.collectAsState()

                    // El campo en curso sobrevive a la recreación por rotación.
                    var campoEscaneado by rememberSaveable { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        viewModel.events.collect { evento ->
                            when (evento) {
                                EstivacionEvent.EnvioExitoso -> {
                                    startActivity(
                                        Intent(
                                            this@EstivacionActivity,
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
                                campo = CampoEstivacion.desdeClave(campoEscaneado),
                                valor = result.data?.getStringExtra("scanResult")
                            )
                        }
                    }

                    when (state.pantalla) {
                        EstivacionPantalla.CARGANDO -> Unit

                        EstivacionPantalla.LISTA_PENDIENTES -> EstivacionListScreen(
                            state = listState,
                            onBack = { finish() },
                            onNewEstivacion = { viewModel.nuevaEstivacion() },
                            onResumeEstivacion = { viewModel.retomarEstivacion(it) },
                            onDeleteEstivacion = { listViewModel.eliminar(it) }
                        )

                        EstivacionPantalla.FORMULARIO -> EstivacionScreen(
                            state = state,
                            onBack = { finish() },
                            onStockearClick = { tipo ->
                                campoEscaneado = tipo
                                scannerLauncher.launch(
                                    Intent(this, BarcodeScannerActivity::class.java)
                                        .putExtra("modo", tipo)
                                )
                            },
                            onProductoChange = viewModel::onPartidaChange,
                            onUbicacionChange = viewModel::onUbicacionChange,
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
        const val EXTRA_UBICACION = "ubicacion"
        const val EXTRA_ID = "id"
    }
}
