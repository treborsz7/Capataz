package com.thinkthat.mamusckascaner.domain.model

/** Orden de trabajo lanzada, tal como se lista para tomarla. */
data class OrdenTrabajoLanzada(
    val id: Int,
    val numero: String,
    val descripcionProducto: String,
    val unificadora: Boolean
)
