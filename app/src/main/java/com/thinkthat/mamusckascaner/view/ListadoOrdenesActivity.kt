package com.thinkthat.mamusckascaner.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.codegalaxy.barcodescanner.view.ListadoOrdenesScreen
import com.thinkthat.mamusckascaner.di.ViewModelFactories
import com.thinkthat.mamusckascaner.presentation.ordenes.ListadoOrdenesViewModel
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme

class ListadoOrdenesActivity : ComponentActivity() {

    private val viewModel: ListadoOrdenesViewModel by viewModels {
        ViewModelFactories.listadoOrdenes
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.state.collectAsState()

                    ListadoOrdenesScreen(
                        state = state,
                        onBack = { finish() },
                        onRecargar = viewModel::cargar,
                        onTomaOrden = { orden ->
                            startActivity(
                                Intent(this, RecolectarActivity::class.java)
                                    .putExtra(RecolectarActivity.EXTRA_ORDEN_ID, orden.id)
                            )
                        }
                    )
                }
            }
        }
    }
}
