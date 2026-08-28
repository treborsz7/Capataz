package com.thinkthat.mamusckascaner.presentation.estivacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkthat.mamusckascaner.domain.model.Estivacion
import com.thinkthat.mamusckascaner.domain.usecase.EliminarEstivacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerEstivacionesPendientesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstivacionListUiState(
    val pendientes: List<Estivacion> = emptyList(),
    val isLoading: Boolean = true
)

/** Lista de estivaciones pendientes de envío. */
class EstivacionListViewModel(
    private val obtenerPendientes: ObtenerEstivacionesPendientesUseCase,
    private val eliminarEstivacion: EliminarEstivacionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EstivacionListUiState())
    val state: StateFlow<EstivacionListUiState> = _state.asStateFlow()

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
            eliminarEstivacion(id)
            val pendientes = obtenerPendientes()
            _state.update { it.copy(pendientes = pendientes) }
        }
    }
}
