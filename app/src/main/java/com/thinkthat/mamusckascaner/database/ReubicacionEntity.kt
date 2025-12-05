package com.thinkthat.mamusckascaner.database

/**
 * Entidad para representar una reubicación guardada
 */
data class ReubicacionEntity(
    val id: Long = 0,
    val partida: String,
    val ubicacionOrigen: String,
    val ubicacionDestino: String,
    val codDeposito: String,
    val fechaCreacion: String,
    val estado: String = "pendiente" // pendiente, sincronizado
)
