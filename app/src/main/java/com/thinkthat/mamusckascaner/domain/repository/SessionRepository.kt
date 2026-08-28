package com.thinkthat.mamusckascaner.domain.repository

import com.thinkthat.mamusckascaner.domain.model.Credenciales
import com.thinkthat.mamusckascaner.domain.model.Sesion

/** Lectura y escritura de la sesión activa (SharedPreferences en Android, Keychain/UserDefaults en iOS). */
interface SessionRepository {

    // --- Lectura ---

    fun sesionActual(): Sesion
    fun codDeposito(): String
    fun usuario(): String

    /**
     * Depósito de la última recolección. Es distinto de [codDeposito]: la
     * recolección toma el depósito del QR y solo cae a este valor si el QR no lo trae.
     */
    fun ultimoDeposito(): String

    /** Credenciales guardadas para el auto-login. */
    fun credenciales(): Credenciales

    // --- Escritura ---

    fun guardarToken(token: String)
    fun guardarEmpresaYDeposito(empresa: String, deposito: String)

    /** Guarda usuario y contraseña para el próximo arranque. */
    fun recordarCredenciales(usuario: String, contrasena: String)

    /** Olvida usuario y contraseña, sin tocar el token ni empresa/depósito. */
    fun olvidarCredenciales()

    /** Cierra sesión: borra credenciales y token; empresa y depósito se mantienen. */
    fun cerrarSesion()
}
