package com.thinkthat.mamusckascaner.presentation.reubicacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.domain.model.Reubicacion
import com.thinkthat.mamusckascaner.domain.repository.SessionRepository
import com.thinkthat.mamusckascaner.domain.usecase.EnviarReubicacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.GuardarBorradorReubicacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerReubicacionesPendientesUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ReubicacionViewModel"

/**
 * Lógica del flujo de reubicación: lista de pendientes, guardado automático del
 * borrador y envío. Sin dependencias de Compose ni de Activity.
 */
class ReubicacionViewModel(
    private val guardarBorrador: GuardarBorradorReubicacionUseCase,
    private val enviarReubicacion: EnviarReubicacionUseCase,
    private val obtenerPendientes: ObtenerReubicacionesPendientesUseCase,
    private val session: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReubicacionUiState())
    val state: StateFlow<ReubicacionUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ReubicacionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ReubicacionEvent> = _events.asSharedFlow()

    private var iniciado = false

    fun iniciar(
        retomar: Boolean,
        id: Long,
        partida: String?,
        ubicacionOrigen: String?,
        ubicacionDestino: String?
    ) {
        if (iniciado) return
        iniciado = true

        _state.update {
            it.copy(
                id = id,
                partida = partida.orEmpty(),
                ubicacionOrigen = ubicacionOrigen.orEmpty(),
                ubicacionDestino = ubicacionDestino.orEmpty(),
                codDeposito = session.codDeposito()
            )
        }

        viewModelScope.launch {
            val hayPendientes = obtenerPendientes().isNotEmpty()
            AppLog.debug(TAG, "Pendientes encontradas: $hayPendientes, retomar: $retomar")
            _state.update {
                it.copy(
                    pantalla = if (hayPendientes && !retomar) {
                        ReubicacionPantalla.LISTA_PENDIENTES
                    } else {
                        ReubicacionPantalla.FORMULARIO
                    }
                )
            }
        }
    }

    fun nuevaReubicacion() {
        _state.update {
            it.copy(
                pantalla = ReubicacionPantalla.FORMULARIO,
                id = -1L,
                partida = "",
                ubicacionOrigen = "",
                ubicacionDestino = "",
                error = null
            )
        }
    }

    fun retomarReubicacion(reubicacion: Reubicacion) {
        _state.update {
            it.copy(
                pantalla = ReubicacionPantalla.FORMULARIO,
                id = reubicacion.id,
                partida = reubicacion.partida,
                ubicacionOrigen = reubicacion.ubicacionOrigen,
                ubicacionDestino = reubicacion.ubicacionDestino,
                error = null
            )
        }
    }

    fun onPartidaChange(valor: String) {
        if (valor == _state.value.partida) return
        _state.update { it.copy(partida = valor, error = null) }
        persistirBorrador()
    }

    fun onUbicacionOrigenChange(valor: String) {
        if (valor == _state.value.ubicacionOrigen) return
        _state.update { it.copy(ubicacionOrigen = valor, error = null) }
        persistirBorrador()
    }

    fun onUbicacionDestinoChange(valor: String) {
        if (valor == _state.value.ubicacionDestino) return
        _state.update { it.copy(ubicacionDestino = valor, error = null) }
        persistirBorrador()
    }

    fun onScanResult(campo: CampoReubicacion?, valor: String?) {
        if (valor.isNullOrBlank()) return
        when (campo) {
            CampoReubicacion.PARTIDA -> onPartidaChange(valor)
            CampoReubicacion.UBICACION_ORIGEN -> onUbicacionOrigenChange(valor)
            CampoReubicacion.UBICACION_DESTINO -> onUbicacionDestinoChange(valor)
            null -> AppLog.debug(TAG, "Resultado de escaneo sin campo asociado: $valor")
        }
    }

    fun enviar() {
        val actual = _state.value
        if (actual.isEnviando) return

        _state.update { it.copy(isEnviando = true, error = null) }
        viewModelScope.launch {
            val resultado = enviarReubicacion(
                actual.id,
                actual.partida,
                actual.ubicacionOrigen,
                actual.ubicacionDestino
            )
            when (resultado) {
                is AppResult.Success -> {
                    _state.update { it.copy(isEnviando = false, error = null) }
                    _events.emit(ReubicacionEvent.EnvioExitoso)
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
            val id = guardarBorrador(
                actual.id,
                actual.partida,
                actual.ubicacionOrigen,
                actual.ubicacionDestino
            )
            if (id != actual.id) {
                _state.update { it.copy(id = id) }
            }
        }
    }
}
