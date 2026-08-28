package com.thinkthat.mamusckascaner.presentation.recolectar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.domain.model.EscaneoRenglon
import com.thinkthat.mamusckascaner.domain.model.RenglonEnvio
import com.thinkthat.mamusckascaner.domain.usecase.EliminarRenglonUseCase
import com.thinkthat.mamusckascaner.domain.usecase.EnviarRecoleccionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.GuardarRenglonUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerUbicacionesParaRecolectarUseCase
import com.thinkthat.mamusckascaner.domain.usecase.RegistrarPedidoUseCase
import com.thinkthat.mamusckascaner.domain.usecase.RestaurarRenglonesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "RecolectarViewModel"

/**
 * Lógica de la recolección de un pedido: cargar ubicaciones, restaurar lo que
 * quedó a medio hacer, ir guardando cada renglón y enviar.
 *
 * La pantalla conserva solo su estado de interacción (qué campo está en edición,
 * qué artículo se está escaneando, qué tarjetas están desplegadas).
 */
class RecolectarViewModel(
    private val obtenerUbicaciones: ObtenerUbicacionesParaRecolectarUseCase,
    private val restaurarRenglones: RestaurarRenglonesUseCase,
    private val registrarPedido: RegistrarPedidoUseCase,
    private val guardarRenglonUseCase: GuardarRenglonUseCase,
    private val eliminarRenglonUseCase: EliminarRenglonUseCase,
    private val enviarRecoleccion: EnviarRecoleccionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RecolectarUiState())
    val state: StateFlow<RecolectarUiState> = _state.asStateFlow()

    private var iniciado = false

    fun iniciar(idPedido: Int, qrDeposito: String?) {
        if (iniciado) return
        iniciado = true
        _state.update { it.copy(idPedido = idPedido, qrDeposito = qrDeposito) }
        cargarUbicaciones()
    }

    /** Recarga las ubicaciones y vuelve a restaurar lo guardado sobre ellas. */
    fun cargarUbicaciones() {
        val idPedido = _state.value.idPedido
        if (idPedido == -1) return

        _state.update { it.copy(isLoadingUbicaciones = true, errorUbicaciones = null) }
        viewModelScope.launch {
            when (val resultado = obtenerUbicaciones(idPedido)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            ubicaciones = resultado.data,
                            isLoadingUbicaciones = false,
                            errorUbicaciones = null
                        )
                    }
                    restaurarEstadoGuardado()
                }
                is AppResult.Failure -> _state.update {
                    it.copy(
                        isLoadingUbicaciones = false,
                        errorUbicaciones = resultado.error.message
                    )
                }
            }
        }
    }

    /**
     * Reconstruye escaneos y cantidades desde lo persistido, después de
     * reconciliarlo contra las ubicaciones vigentes.
     */
    private fun restaurarEstadoGuardado() {
        val actual = _state.value
        viewModelScope.launch {
            val renglones = restaurarRenglones(actual.idPedido, actual.ubicaciones)
            if (renglones.isEmpty()) return@launch

            val escaneos = mutableMapOf<String, List<EscaneoRenglon>>()
            val cantidades = mutableMapOf<String, Map<Int, String>>()
            val guardadas = mutableMapOf<String, Map<Int, Boolean>>()

            renglones.groupBy { it.codArticulo }.forEach { (codArticulo, delArticulo) ->
                val ordenados = delArticulo.sortedBy { it.indiceScaneo }
                escaneos[codArticulo] = ordenados.map {
                    EscaneoRenglon(partida = it.partida, ubicacion = it.ubicacion)
                }
                cantidades[codArticulo] = ordenados.withIndex().associate { (indice, renglon) ->
                    indice to renglon.cantidad.sinDecimalesSiEsEntero()
                }
                guardadas[codArticulo] = ordenados.indices.associateWith { true }
            }

            _state.update {
                it.copy(
                    escaneos = escaneos,
                    cantidades = cantidades,
                    cantidadesGuardadas = guardadas
                )
            }
            AppLog.debug(TAG, "Estado restaurado: ${renglones.size} renglones")
        }
    }

    // --- Edición de renglones ---

    fun onPartidaEscaneada(codArticulo: String, indice: Int, partida: String) {
        actualizarEscaneo(codArticulo, indice) { it.copy(partida = partida) }
    }

    fun onUbicacionEscaneada(codArticulo: String, indice: Int, ubicacion: String) {
        actualizarEscaneo(codArticulo, indice) { it.copy(ubicacion = ubicacion) }
    }

    fun onCantidadChange(codArticulo: String, indice: Int, cantidad: String) {
        _state.update { estado ->
            val delArticulo = estado.cantidades[codArticulo].orEmpty() + (indice to cantidad)
            estado.copy(cantidades = estado.cantidades + (codArticulo to delArticulo))
        }
    }

    /** Asegura que exista el escaneo [indice] del artículo, para poder editarlo. */
    fun asegurarEscaneos(codArticulo: String, cantidadDeEscaneos: Int) {
        _state.update { estado ->
            val actuales = estado.escaneos[codArticulo].orEmpty()
            if (actuales.size >= cantidadDeEscaneos) return@update estado
            val completados = actuales + List(cantidadDeEscaneos - actuales.size) { EscaneoRenglon() }
            estado.copy(escaneos = estado.escaneos + (codArticulo to completados))
        }
    }

    /**
     * Confirma un renglón: lo marca como guardado y lo persiste. Se llama tanto
     * al guardar la partida como al guardar la cantidad.
     */
    fun guardarRenglon(codArticulo: String, indice: Int, marcarCantidadGuardada: Boolean) {
        if (marcarCantidadGuardada) {
            _state.update { estado ->
                val delArticulo = estado.cantidadesGuardadas[codArticulo].orEmpty() + (indice to true)
                estado.copy(cantidadesGuardadas = estado.cantidadesGuardadas + (codArticulo to delArticulo))
            }
        }

        val actual = _state.value
        val escaneo = actual.escaneosDe(codArticulo).getOrNull(indice) ?: return
        val ubicacionDelArticulo = actual.ubicacionesDe(codArticulo).firstOrNull()

        viewModelScope.launch {
            guardarRenglonUseCase(
                idPedido = actual.idPedido,
                qrDeposito = actual.qrDeposito,
                codArticulo = codArticulo,
                nombreArticulo = ubicacionDelArticulo?.descripcionArticulo ?: SIN_DATO,
                cantidadSolicitada = ubicacionDelArticulo?.requerido ?: 0.0,
                indiceScaneo = indice,
                partida = escaneo.partida,
                ubicacion = escaneo.ubicacion,
                cantidad = actual.cantidadDe(codArticulo, indice).toDoubleOrNull() ?: 0.0
            )
            registrarPedido(actual.idPedido, actual.qrDeposito, actual.ubicaciones)
        }
    }

    /** Vuelve a dejar editable una cantidad ya confirmada. */
    fun onEditarCantidad(codArticulo: String, indice: Int) {
        _state.update { estado ->
            val delArticulo = estado.cantidadesGuardadas[codArticulo].orEmpty() + (indice to false)
            estado.copy(
                cantidadesGuardadas = estado.cantidadesGuardadas + (codArticulo to delArticulo)
            )
        }
    }

    /** Elimina un escaneo y reindexa los que quedan, en memoria y en la base. */
    fun eliminarRenglon(codArticulo: String, indice: Int) {
        val actual = _state.value
        viewModelScope.launch {
            eliminarRenglonUseCase(actual.idPedido, codArticulo, indice)
        }

        _state.update { estado ->
            val escaneos = estado.escaneos[codArticulo].orEmpty().toMutableList()
            if (indice < escaneos.size) escaneos.removeAt(indice)

            estado.copy(
                escaneos = estado.escaneos + (codArticulo to escaneos),
                cantidades = estado.cantidades +
                    (codArticulo to estado.cantidades[codArticulo].orEmpty().reindexar(indice)),
                cantidadesGuardadas = estado.cantidadesGuardadas +
                    (codArticulo to estado.cantidadesGuardadas[codArticulo].orEmpty().reindexar(indice))
            )
        }
    }

    // --- Envío ---

    fun enviar() {
        val actual = _state.value
        if (actual.isEnviando) return

        val renglones = actual.aRenglonesDeEnvio()
        _state.update { it.copy(isEnviando = true, errorEnvio = null) }

        viewModelScope.launch {
            val resultado = enviarRecoleccion(actual.idPedido, actual.qrDeposito, renglones)
            when (resultado) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        isEnviando = false,
                        errorEnvio = null,
                        envioExitoso = true,
                        escaneos = emptyMap(),
                        cantidades = emptyMap(),
                        cantidadesGuardadas = emptyMap()
                    )
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isEnviando = false, errorEnvio = resultado.error.message)
                }
            }
        }
    }

    fun limpiarError() {
        _state.update { it.copy(errorEnvio = null) }
    }

    private fun actualizarEscaneo(
        codArticulo: String,
        indice: Int,
        transformar: (EscaneoRenglon) -> EscaneoRenglon
    ) {
        _state.update { estado ->
            val lista = estado.escaneos[codArticulo].orEmpty().toMutableList()
            while (lista.size <= indice) lista.add(EscaneoRenglon())
            lista[indice] = transformar(lista[indice])
            estado.copy(escaneos = estado.escaneos + (codArticulo to lista))
        }
    }

    private companion object {
        const val SIN_DATO = "N/A"
    }
}

/**
 * Solo se envían los escaneos con cantidad confirmada, ubicación y cantidad
 * mayor a cero. La partida puede ir vacía: no todas las ubicaciones la exigen.
 */
private fun RecolectarUiState.aRenglonesDeEnvio(): List<RenglonEnvio> =
    escaneos.flatMap { (codArticulo, lista) ->
        lista.mapIndexedNotNull { indice, escaneo ->
            val cantidad = cantidadDe(codArticulo, indice).toDoubleOrNull() ?: 0.0
            if (!cantidadGuardada(codArticulo, indice) ||
                escaneo.ubicacion.isEmpty() ||
                cantidad <= 0
            ) {
                null
            } else {
                RenglonEnvio(
                    codArticulo = codArticulo,
                    nombreUbicacion = escaneo.ubicacion,
                    numPartida = escaneo.partida,
                    cantidad = cantidad
                )
            }
        }
    }

/** Quita la entrada [indice] y corre hacia atrás las posteriores. */
private fun <T> Map<Int, T>.reindexar(indice: Int): Map<Int, T> =
    filterKeys { it != indice }
        .mapKeys { (clave, _) -> if (clave > indice) clave - 1 else clave }

private fun Double.sinDecimalesSiEsEntero(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
