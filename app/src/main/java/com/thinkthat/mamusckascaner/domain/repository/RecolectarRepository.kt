package com.thinkthat.mamusckascaner.domain.repository

import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.domain.model.EnvioRecoleccion
import com.thinkthat.mamusckascaner.domain.model.PedidoRecoleccion
import com.thinkthat.mamusckascaner.domain.model.RenglonRecoleccion
import com.thinkthat.mamusckascaner.domain.model.UbicacionRecolectar

/**
 * Acceso a la recolección de pedidos: ubicaciones desde la API, renglones
 * persistidos localmente y envío final.
 */
interface RecolectarRepository {

    suspend fun ubicacionesParaRecolectar(
        idPedido: Int,
        optimizaRecorrido: Boolean = false
    ): AppResult<List<UbicacionRecolectar>>

    // --- Renglones locales ---

    suspend fun renglonesDelPedido(idPedido: Int): List<RenglonRecoleccion>
    suspend fun guardarRenglon(renglon: RenglonRecoleccion): Long
    suspend fun actualizarRenglon(renglon: RenglonRecoleccion): Boolean
    suspend fun eliminarRenglon(id: Long): Boolean
    suspend fun marcarRenglonSincronizado(id: Long): Boolean

    // --- Pedido local ---

    suspend fun pedido(idPedido: Int): PedidoRecoleccion?

    /** Pedidos guardados que todavía no se sincronizaron. */
    suspend fun pedidosPendientes(): List<PedidoRecoleccion>

    /** Borra el pedido junto con todos sus renglones. */
    suspend fun eliminarPedidoConRenglones(idPedido: Int): Boolean
    suspend fun guardarPedido(pedido: PedidoRecoleccion): Long
    suspend fun actualizarEstadoPedido(idPedido: Int, estado: String): Boolean

    /** Serializa las ubicaciones para dejarlas junto al pedido guardado. */
    fun serializarUbicaciones(ubicaciones: List<UbicacionRecolectar>): String

    // --- Envío ---

    suspend fun enviarRecoleccion(envio: EnvioRecoleccion): AppResult<Unit>
}
