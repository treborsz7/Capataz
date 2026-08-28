package com.thinkthat.mamusckascaner.domain.model

/**
 * El traslado de una partida entre dos ubicaciones del mismo depósito.
 */
data class Reubicacion(
    val id: Long = 0,
    val partida: String,
    val ubicacionOrigen: String,
    val ubicacionDestino: String,
    val codDeposito: String,
    val fechaCreacion: String,
    val estado: String = EstadoRegistro.PENDIENTE
) {
    val esEnviable: Boolean
        get() = partida.isNotBlank() &&
            ubicacionOrigen.isNotBlank() &&
            ubicacionDestino.isNotBlank() &&
            codDeposito.isNotBlank()
}
