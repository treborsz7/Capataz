package com.thinkthat.mamusckascaner.presentation.ordenes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.domain.model.OrdenTrabajoLanzada
import com.thinkthat.mamusckascaner.domain.repository.OrdenesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListadoOrdenesUiState(
    val ordenes: List<OrdenTrabajoLanzada> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/** Listado de órdenes lanzadas disponibles para tomar. */
class ListadoOrdenesViewModel(
    private val repository: OrdenesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ListadoOrdenesUiState())
    val state: StateFlow<ListadoOrdenesUiState> = _state.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val resultado = repository.ordenesLanzadas()) {
                is AppResult.Success -> _state.update {
                    it.copy(ordenes = resultado.data, isLoading = false, error = null)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = resultado.error.message)
                }
            }
        }
    }
}
