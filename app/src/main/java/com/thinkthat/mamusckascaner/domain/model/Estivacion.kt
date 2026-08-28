package com.thinkthat.mamusckascaner.domain.model

/**
 * Una partida asignada a una ubicación de depósito.
 * [id] es 0 mientras no se haya persistido localmente.
 */
data class Estivacion(
    val id: Long = 0,
    val partida: String,
    val ubicacion: String,
    val codDeposito: String,
    val fechaCreacion: String,
    val estado: String = EstadoRegistro.PENDIENTE
) {
    /** Una estivación solo se puede enviar con partida y ubicación cargadas. */
    val esEnviable: Boolean
        get() = partida.isNotBlank() && ubicacion.isNotBlank()
}
