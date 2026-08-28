package com.thinkthat.mamusckascaner.domain.model

/** Credenciales guardadas para el auto-login. */
data class Credenciales(
    val usuario: String = "",
    val contrasena: String = "",
    val recordar: Boolean = false
) {
    /** Solo sirven para auto-login si están completas y el usuario pidió recordarlas. */
    val permitenAutoLogin: Boolean
        get() = recordar && usuario.isNotBlank() && contrasena.isNotBlank()
}
