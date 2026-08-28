package com.thinkthat.mamusckascaner.domain.repository

import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.domain.model.Reubicacion

interface ReubicacionRepository {
    suspend fun insertar(reubicacion: Reubicacion): Long
    suspend fun actualizar(reubicacion: Reubicacion): Boolean
    suspend fun pendientes(): List<Reubicacion>
    suspend fun eliminar(id: Long): Boolean

    /** Envía la reubicación a la API. No toca la persistencia local. */
    suspend fun enviar(reubicacion: Reubicacion): AppResult<Unit>
}
