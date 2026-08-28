package com.thinkthat.mamusckascaner.domain.repository

import com.thinkthat.mamusckascaner.core.AppResult

/** Autenticación contra la API. */
interface AuthRepository {
    /** Devuelve el token en crudo tal como lo entrega el endpoint de login. */
    suspend fun login(usuario: String, contrasena: String): AppResult<String>
}
