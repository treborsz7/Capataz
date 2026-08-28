package com.thinkthat.mamusckascaner.data.mapper

import com.thinkthat.mamusckascaner.database.EstivacionEntity
import com.thinkthat.mamusckascaner.database.ReubicacionEntity
import com.thinkthat.mamusckascaner.domain.model.Estivacion
import com.thinkthat.mamusckascaner.domain.model.Reubicacion
import com.thinkthat.mamusckascaner.domain.model.Ubicacion
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
