package com.thinkthat.mamusckascaner.database

/**
 * Entidad para pedidos guardados localmente
 */
data class PedidoEntity(
    val id: Long = 0,
    val idPedido: Int,
    val codDeposito: String,
    val fechaCreacion: String,
    val estado: String = "pendiente", // pendiente, completado, sincronizado
    val ubicacionesJson: String = "" // JSON con las ubicaciones
)
