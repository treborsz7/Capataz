package com.thinkthat.mamusckascaner.domain.repository

import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.domain.model.Ubicacion

interface UbicacionRepository {
    suspend fun ubicacionesParaEstibar(
        codDeposito: String,
        codArticulo: String?,
        optimizaRecorrido: Boolean = true
    ): AppResult<List<Ubicacion>>
}
