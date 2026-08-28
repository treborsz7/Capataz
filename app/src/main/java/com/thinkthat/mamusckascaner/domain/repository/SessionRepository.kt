package com.thinkthat.mamusckascaner.domain.repository

import com.thinkthat.mamusckascaner.domain.model.Sesion

/** Lectura de la sesión activa (SharedPreferences en Android, Keychain/UserDefaults en iOS). */
interface SessionRepository {
    fun sesionActual(): Sesion
    fun codDeposito(): String
}
