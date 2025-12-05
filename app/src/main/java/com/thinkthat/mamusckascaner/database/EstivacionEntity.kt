package com.thinkthat.mamusckascaner.database

/**
 * Entidad para representar una estivación guardada
 */
data class EstivacionEntity(
    val id: Long = 0,
    val partida: String,
    val ubicacion: String,
    val codDeposito: String,
    val fechaCreacion: String,
    val estado: String = "pendiente" // pendiente, sincronizado
)
