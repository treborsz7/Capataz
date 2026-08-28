package com.thinkthat.mamusckascaner.domain.repository

import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.domain.model.OrdenTrabajoLanzada

interface OrdenesRepository {
    suspend fun ordenesLanzadas(): AppResult<List<OrdenTrabajoLanzada>>
}
