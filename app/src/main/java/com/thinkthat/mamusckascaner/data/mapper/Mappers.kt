package com.thinkthat.mamusckascaner.data.mapper

import com.thinkthat.mamusckascaner.database.EstivacionEntity
import com.thinkthat.mamusckascaner.database.PedidoEntity
import com.thinkthat.mamusckascaner.database.RecoleccionEntity
import com.thinkthat.mamusckascaner.database.ReubicacionEntity
import com.thinkthat.mamusckascaner.domain.model.Estivacion
import com.thinkthat.mamusckascaner.domain.model.OrdenTrabajoLanzada
import com.thinkthat.mamusckascaner.domain.model.PedidoRecoleccion
import com.thinkthat.mamusckascaner.domain.model.RenglonRecoleccion
import com.thinkthat.mamusckascaner.domain.model.Reubicacion
import com.thinkthat.mamusckascaner.domain.model.Ubicacion
import com.thinkthat.mamusckascaner.domain.model.UbicacionRecolectar
import com.thinkthat.mamusckascaner.service.Services.OrdenLanzada
import com.thinkthat.mamusckascaner.service.Services.UbicacionResponse

// --- Estivación ---

fun EstivacionEntity.toDomain(): Estivacion = Estivacion(
    id = id,
    partida = partida,
    ubicacion = ubicacion,
    codDeposito = codDeposito,
    fechaCreacion = fechaCreacion,
    estado = estado
)

fun Estivacion.toEntity(): EstivacionEntity = EstivacionEntity(
    id = id,
    partida = partida,
    ubicacion = ubicacion,
    codDeposito = codDeposito,
    fechaCreacion = fechaCreacion,
    estado = estado
)

// --- Reubicación ---

fun ReubicacionEntity.toDomain(): Reubicacion = Reubicacion(
    id = id,
    partida = partida,
    ubicacionOrigen = ubicacionOrigen,
    ubicacionDestino = ubicacionDestino,
    codDeposito = codDeposito,
    fechaCreacion = fechaCreacion,
    estado = estado
)

fun Reubicacion.toEntity(): ReubicacionEntity = ReubicacionEntity(
    id = id,
    partida = partida,
    ubicacionOrigen = ubicacionOrigen,
    ubicacionDestino = ubicacionDestino,
    codDeposito = codDeposito,
    fechaCreacion = fechaCreacion,
    estado = estado
)

// --- Ubicación (DTO de red -> dominio) ---

fun UbicacionResponse.toDomain(): Ubicacion = Ubicacion(
    numero = numero,
    nombre = nombre,
    alias = alias,
    orden = orden
)

// --- Recolección ---

fun RecoleccionEntity.toDomain(): RenglonRecoleccion = RenglonRecoleccion(
    id = id,
    idPedido = idPedido,
    codArticulo = codArticulo,
    nombreArticulo = nombreArticulo,
    cantidadSolicitada = cantidadSolicitada,
    ubicacion = ubicacion,
    partida = partida,
    cantidad = cantidad,
    codDeposito = codDeposito,
    usuario = usuario,
    fechaHora = fechaHora,
    sincronizado = sincronizado,
    indiceScaneo = indiceScaneo
)

fun RenglonRecoleccion.toEntity(): RecoleccionEntity = RecoleccionEntity(
    id = id,
    idPedido = idPedido,
    codArticulo = codArticulo,
    nombreArticulo = nombreArticulo,
    cantidadSolicitada = cantidadSolicitada,
    ubicacion = ubicacion,
    partida = partida,
    cantidad = cantidad,
    codDeposito = codDeposito,
    usuario = usuario,
    fechaHora = fechaHora,
    sincronizado = sincronizado,
    indiceScaneo = indiceScaneo
)

fun PedidoEntity.toDomain(): PedidoRecoleccion = PedidoRecoleccion(
    id = id,
    idPedido = idPedido,
    codDeposito = codDeposito,
    fechaCreacion = fechaCreacion,
    estado = estado,
    ubicacionesJson = ubicacionesJson
)

fun PedidoRecoleccion.toEntity(): PedidoEntity = PedidoEntity(
    id = id,
    idPedido = idPedido,
    codDeposito = codDeposito,
    fechaCreacion = fechaCreacion,
    estado = estado,
    ubicacionesJson = ubicacionesJson
)

/**
 * Aplana las ubicaciones de la API (cada una con sus artículos anidados) a una
 * fila por artículo, que es la forma en que se listan para recolectar.
 * Las ubicaciones sin artículos se descartan, igual que antes.
 */
fun List<UbicacionResponse>.toUbicacionesRecolectar(): List<UbicacionRecolectar> =
    flatMap { ubicacion ->
        ubicacion.articulos.orEmpty().map { articulo ->
            UbicacionRecolectar(
                numeroUbicacion = ubicacion.numero.toString(),
                nombreUbicacion = ubicacion.nombre,
                descripcionArticulo = articulo.descripcion,
                codArticulo = articulo.codigo,
                requerido = articulo.requerido ?: 0.0,
                saldoDisponible = articulo.saldoDisponible ?: 0.0,
                nroPartida = articulo.nroPartida ?: SIN_DATO
            )
        }
    }

private const val SIN_DATO = "N/A"

// --- Órdenes ---

fun OrdenLanzada.toDomain(): OrdenTrabajoLanzada = OrdenTrabajoLanzada(
    id = id,
    numero = numero,
    descripcionProducto = producto.descripcion,
    unificadora = unificadora
)
