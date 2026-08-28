package com.thinkthat.mamusckascaner.presentation.reubicacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkthat.mamusckascaner.domain.model.Reubicacion
import com.thinkthat.mamusckascaner.domain.usecase.EliminarReubicacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerReubicacionesPendientesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReubicacionListUiState(
    val pendientes: List<Reubicacion> = emptyList(),
    val isLoading: Boolean = true
)

/** Lista de reubicaciones pendientes de envío. */
class ReubicacionListViewModel(
    private val obtenerPendientes: ObtenerReubicacionesPendientesUseCase,
    private val eliminarReubicacion: EliminarReubicacionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ReubicacionListUiState())
    val state: StateFlow<ReubicacionListUiState> = _state.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val pendientes = obtenerPendientes()
            _state.update { it.copy(pendientes = pendientes, isLoading = false) }
        }
    }

    fun eliminar(id: Long) {
        viewModelScope.launch {
            eliminarReubicacion(id)
            val pendientes = obtenerPendientes()
            _state.update { it.copy(pendientes = pendientes) }
        }
    }
}
