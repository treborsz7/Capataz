package com.thinkthat.mamusckascaner.domain.usecase

import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.domain.model.Ubicacion
import com.thinkthat.mamusckascaner.domain.repository.SessionRepository
import com.thinkthat.mamusckascaner.domain.repository.UbicacionRepository

/**
 * Ubicaciones sugeridas para estibar una partida en el depósito de la sesión.
 */
class ObtenerUbicacionesParaEstibarUseCase(
    private val repository: UbicacionRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(codArticulo: String?): AppResult<List<Ubicacion>> =
        repository.ubicacionesParaEstibar(
            codDeposito = session.codDeposito(),
            codArticulo = codArticulo,
            optimizaRecorrido = true
        )
}
