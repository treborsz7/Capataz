package com.thinkthat.mamusckascaner.domain.model

/** Ubicación de depósito devuelta por la API. */
data class Ubicacion(
    val numero: Int,
    val nombre: String,
    val alias: String = "",
    val orden: Int = 0
)
