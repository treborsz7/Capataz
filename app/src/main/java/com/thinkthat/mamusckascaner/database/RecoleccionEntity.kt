package com.thinkthat.mamusckascaner.database

/**
 * Entidad que representa una recolección guardada localmente
 */
data class RecoleccionEntity(
    val id: Long = 0,
    val idPedido: Int,
    val codArticulo: String,
    val nombreArticulo: String,
    val cantidadSolicitada: Int,
    val ubicacion: String,
    val partida: String,
    val cantidad: Int,
    val codDeposito: String,
    val usuario: String,
    val fechaHora: String,
    val sincronizado: Boolean = false,
    val indiceScaneo: Int = 0 // Para identificar múltiples escaneos del mismo artículo
)
