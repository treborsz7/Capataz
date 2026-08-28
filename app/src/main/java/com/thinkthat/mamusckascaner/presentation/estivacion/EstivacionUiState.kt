package com.thinkthat.mamusckascaner.presentation.estivacion

import com.thinkthat.mamusckascaner.domain.model.Ubicacion

/** Qué se está mostrando dentro del flujo de estivación. */
enum class EstivacionPantalla { CARGANDO, LISTA_PENDIENTES, FORMULARIO }

/**
 * Estado completo de la pantalla de estivación. Todo lo que la UI necesita
 * dibujar sale de acá; la UI no consulta ni la base ni la API.
 */
data class EstivacionUiState(
    val pantalla: EstivacionPantalla = EstivacionPantalla.CARGANDO,
    val id: Long = -1L,
    val partida: String = "",
    val ubicacion: String = "",
    val ubicacionesSugeridas: List<Ubicacion> = emptyList(),
    val isEnviando: Boolean = false,
    val error: String? = null
) {
    val puedeEnviar: Boolean
        get() = partida.isNotBlank() && ubicacion.isNotBlank() && !isEnviando
}

/** Efectos de una sola vez: la navegación la resuelve la plataforma. */
sealed interface EstivacionEvent {
    data object EnvioExitoso : EstivacionEvent
}

/** Campo que se está escaneando con la cámara. */
enum class CampoEstivacion(val clave: String) {
    PARTIDA("producto"),
    UBICACION("ubicacion");

    companion object {
        fun desdeClave(clave: String?): CampoEstivacion? =
            entries.firstOrNull { it.clave == clave }
    }
}
