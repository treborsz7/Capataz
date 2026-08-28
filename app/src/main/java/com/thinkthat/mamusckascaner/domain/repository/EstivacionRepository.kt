package com.thinkthat.mamusckascaner.domain.repository

import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.domain.model.Estivacion

/**
 * Acceso a estivaciones: borradores locales + envío a la API.
 * La implementación vive en la capa data y es distinta por plataforma.
 */
interface EstivacionRepository {
    suspend fun insertar(estivacion: Estivacion): Long
    suspend fun actualizar(estivacion: Estivacion): Boolean
    suspend fun pendientes(): List<Estivacion>
    suspend fun eliminar(id: Long): Boolean

    /** Envía la estivación a la API. No toca la persistencia local. */
    suspend fun enviar(estivacion: Estivacion): AppResult<Unit>
}
