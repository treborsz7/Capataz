package com.thinkthat.mamusckascaner.presentation.recolectar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.domain.model.PedidoRecoleccion
import com.thinkthat.mamusckascaner.domain.model.parsearQr
import com.thinkthat.mamusckascaner.domain.usecase.EliminarPedidoUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerPedidosPendientesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "RecolectarQrViewModel"

data class RecolectarQrUiState(
    val pedidosPendientes: List<PedidoRecoleccion> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    /** QR válido listo para abrir la recolección; se limpia con [qrConsumido]. */
    val qrAceptado: String? = null
)

/**
 * Pantalla previa a la recolección: lista los pedidos a medio hacer y valida el
 * QR escaneado antes de dejar entrar.
 */
class RecolectarQrViewModel(
    private val obtenerPendientes: ObtenerPedidosPendientesUseCase,
    private val eliminarPedido: EliminarPedidoUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RecolectarQrUiState())
    val state: StateFlow<RecolectarQrUiState> = _state.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val pedidos = obtenerPendientes()
            AppLog.debug(TAG, "Pedidos pendientes: ${pedidos.size}")
            _state.update { it.copy(pedidosPendientes = pedidos, isLoading = false) }
        }
    }

    /** Valida el contenido del QR: si sirve, habilita la navegación. */
    fun onQrEscaneado(contenido: String?) {
        if (contenido == null) {
            _state.update { it.copy(error = null, qrAceptado = null) }
            return
        }

        if (parsearQr(contenido).esValido) {
            _state.update { it.copy(error = null, qrAceptado = contenido) }
        } else {
            AppLog.error(TAG, "QR inválido sin datos esperados: $contenido")
            _state.update {
                it.copy(error = "No se pudo procesar el QR escaneado.", qrAceptado = null)
            }
        }
    }

    /** La plataforma avisa que ya navegó con el QR aceptado. */
    fun qrConsumido() {
        _state.update { it.copy(qrAceptado = null) }
    }

    fun eliminar(idPedido: Int) {
        viewModelScope.launch {
            eliminarPedido(idPedido)
            _state.update { it.copy(pedidosPendientes = obtenerPendientes()) }
        }
    }
}
