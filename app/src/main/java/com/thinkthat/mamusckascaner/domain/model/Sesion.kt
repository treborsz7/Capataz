package com.thinkthat.mamusckascaner.domain.model

/** Datos de la sesión iniciada, leídos del almacenamiento de la plataforma. */
data class Sesion(
    val usuario: String,
    val empresa: String,
    val codDeposito: String
)
