package com.thinkthat.mamusckascaner.presentation.reubicacion

/** Qué se está mostrando dentro del flujo de reubicación. */
enum class ReubicacionPantalla { CARGANDO, LISTA_PENDIENTES, FORMULARIO }

/**
 * Estado completo de la pantalla de reubicación.
 * [codDeposito] se expone porque el botón de envío solo aparece con depósito cargado.
 */
data class ReubicacionUiState(
    val pantalla: ReubicacionPantalla = ReubicacionPantalla.CARGANDO,
    val id: Long = -1L,
    val partida: String = "",
    val ubicacionOrigen: String = "",
    val ubicacionDestino: String = "",
    val codDeposito: String = "",
    val isEnviando: Boolean = false,
    val error: String? = null
) {
    val puedeEnviar: Boolean
        get() = partida.isNotBlank() &&
            ubicacionOrigen.isNotBlank() &&
            ubicacionDestino.isNotBlank() &&
            codDeposito.isNotBlank() &&
            !isEnviando
}

sealed interface ReubicacionEvent {
    data object EnvioExitoso : ReubicacionEvent
}

/** Campo que se está escaneando con la cámara. */
enum class CampoReubicacion(val clave: String) {
    PARTIDA("producto"),
    UBICACION_ORIGEN("ubicacion_origen"),
    UBICACION_DESTINO("ubicacion_destino");

    companion object {
        fun desdeClave(clave: String?): CampoReubicacion? =
            entries.firstOrNull { it.clave == clave }
    }
}
