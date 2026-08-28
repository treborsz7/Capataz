package com.thinkthat.mamusckascaner.presentation.estivacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.domain.model.Estivacion
import com.thinkthat.mamusckascaner.domain.usecase.EnviarEstivacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.GuardarBorradorEstivacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerEstivacionesPendientesUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerUbicacionesParaEstibarUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "EstivacionViewModel"

/**
 * Toda la lógica del flujo de estivación: decidir si arrancar en la lista de
 * pendientes o en el formulario, guardar el borrador a medida que se completan
 * los campos, y enviar.
 *
 * No conoce Compose, ni Activity, ni Intents: se comunica por [state] y [events].
 */
class EstivacionViewModel(
    private val guardarBorrador: GuardarBorradorEstivacionUseCase,
    private val enviarEstivacion: EnviarEstivacionUseCase,
    private val obtenerPendientes: ObtenerEstivacionesPendientesUseCase,
    private val obtenerUbicaciones: ObtenerUbicacionesParaEstibarUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EstivacionUiState())
    val state: StateFlow<EstivacionUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<EstivacionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EstivacionEvent> = _events.asSharedFlow()

    private var iniciado = false

    /**
     * Punto de entrada desde la plataforma. Si [retomar] es true se abre
     * directamente el formulario con los datos recibidos; si no, se abre la
     * lista de pendientes cuando hay borradores guardados.
     */
    fun iniciar(retomar: Boolean, id: Long, partida: String?, ubicacion: String?) {
        if (iniciado) return
        iniciado = true

        _state.update {
            it.copy(
                id = id,
                partida = partida.orEmpty(),
                ubicacion = ubicacion.orEmpty()
            )
        }

        viewModelScope.launch {
            val hayPendientes = obtenerPendientes().isNotEmpty()
            AppLog.debug(TAG, "Pendientes encontradas: $hayPendientes, retomar: $retomar")
            _state.update {
                it.copy(
                    pantalla = if (hayPendientes && !retomar) {
                        EstivacionPantalla.LISTA_PENDIENTES
                    } else {
                        EstivacionPantalla.FORMULARIO
                    }
                )
            }
        }
    }

    fun nuevaEstivacion() {
        _state.update {
            it.copy(
                pantalla = EstivacionPantalla.FORMULARIO,
                id = -1L,
                partida = "",
                ubicacion = "",
                error = null
            )
        }
    }

    fun retomarEstivacion(estivacion: Estivacion) {
        _state.update {
            it.copy(
                pantalla = EstivacionPantalla.FORMULARIO,
                id = estivacion.id,
                partida = estivacion.partida,
                ubicacion = estivacion.ubicacion,
                error = null
            )
        }
    }

    fun onPartidaChange(valor: String) {
        if (valor == _state.value.partida) return
        _state.update { it.copy(partida = valor, error = null) }
        persistirBorrador()
    }

    fun onUbicacionChange(valor: String) {
        if (valor == _state.value.ubicacion) return
        _state.update { it.copy(ubicacion = valor, error = null) }
        persistirBorrador()
    }

    /**
     * Resultado del escáner. Además de cargar el campo, al escanear una partida
     * se piden las ubicaciones sugeridas para estibarla.
     */
    fun onScanResult(campo: CampoEstivacion?, valor: String?) {
        if (valor.isNullOrBlank()) return
        when (campo) {
            CampoEstivacion.PARTIDA -> {
                onPartidaChange(valor)
                cargarUbicacionesSugeridas(valor)
            }
            CampoEstivacion.UBICACION -> onUbicacionChange(valor)
            null -> AppLog.debug(TAG, "Resultado de escaneo sin campo asociado: $valor")
        }
    }

    fun enviar() {
        val actual = _state.value
        if (actual.isEnviando) return

        _state.update { it.copy(isEnviando = true, error = null) }
        viewModelScope.launch {
            when (val resultado = enviarEstivacion(actual.id, actual.partida, actual.ubicacion)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isEnviando = false, error = null) }
                    _events.emit(EstivacionEvent.EnvioExitoso)
                }
                is AppResult.Failure -> {
                    _state.update {
                        it.copy(isEnviando = false, error = resultado.error.message)
                    }
                }
            }
        }
    }

    fun limpiarError() {
        _state.update { it.copy(error = null) }
    }

    private fun persistirBorrador() {
        val actual = _state.value
        viewModelScope.launch {
            val id = guardarBorrador(actual.id, actual.partida, actual.ubicacion)
            if (id != actual.id) {
                _state.update { it.copy(id = id) }
            }
        }
    }

    private fun cargarUbicacionesSugeridas(codArticulo: String) {
        viewModelScope.launch {
            when (val resultado = obtenerUbicaciones(codArticulo)) {
                is AppResult.Success ->
                    _state.update { it.copy(ubicacionesSugeridas = resultado.data) }
                is AppResult.Failure ->
                    // Es información complementaria: no bloquea el flujo de carga.
                    AppLog.error(TAG, "No se pudieron cargar ubicaciones: ${resultado.error.message}")
            }
        }
    }
}
