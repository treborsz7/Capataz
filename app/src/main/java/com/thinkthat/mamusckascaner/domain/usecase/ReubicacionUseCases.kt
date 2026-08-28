package com.thinkthat.mamusckascaner.domain.usecase

import com.thinkthat.mamusckascaner.core.AppError
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.core.Clock
import com.thinkthat.mamusckascaner.domain.model.Reubicacion
import com.thinkthat.mamusckascaner.domain.repository.ReubicacionRepository
import com.thinkthat.mamusckascaner.domain.repository.SessionRepository

private const val TAG = "ReubicacionUseCase"

/**
 * Guarda el borrador de la reubicación en curso; inserta o actualiza según [id]
 * y devuelve el id vigente.
 */
class GuardarBorradorReubicacionUseCase(
    private val repository: ReubicacionRepository,
    private val session: SessionRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(
        id: Long,
        partida: String,
        ubicacionOrigen: String,
        ubicacionDestino: String
    ): Long {
        if (partida.isBlank() && ubicacionOrigen.isBlank() && ubicacionDestino.isBlank()) return id

        val reubicacion = Reubicacion(
            id = if (id > 0) id else 0,
            partida = partida,
            ubicacionOrigen = ubicacionOrigen,
            ubicacionDestino = ubicacionDestino,
            codDeposito = session.codDeposito(),
            fechaCreacion = clock.nowLocal()
        )

        return try {
            if (id > 0) {
                repository.actualizar(reubicacion)
                id
            } else {
                repository.insertar(reubicacion)
            }
        } catch (e: Exception) {
            AppLog.error(TAG, "Error al guardar el borrador de reubicación", e)
            id
        }
    }
}

/**
 * Envía la reubicación a la API y, si el envío fue exitoso, borra el borrador local.
 */
class EnviarReubicacionUseCase(
    private val repository: ReubicacionRepository,
    private val session: SessionRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(
        id: Long,
        partida: String,
        ubicacionOrigen: String,
        ubicacionDestino: String
    ): AppResult<Unit> {
        val reubicacion = Reubicacion(
            id = id,
            partida = partida,
            ubicacionOrigen = ubicacionOrigen,
            ubicacionDestino = ubicacionDestino,
            codDeposito = session.codDeposito(),
            fechaCreacion = clock.nowLocal()
        )

        if (!reubicacion.esEnviable) {
            return AppResult.Failure(
                AppError("Faltan datos: se requieren partida, origen, destino y depósito.")
            )
        }

        AppLog.info(
            TAG,
            "Iniciando reubicación - Partida: $partida, Origen: $ubicacionOrigen, " +
                "Destino: $ubicacionDestino, Depósito: ${reubicacion.codDeposito}"
        )

        val resultado = repository.enviar(reubicacion)
        if (resultado is AppResult.Success && id > 0) {
            try {
                repository.eliminar(id)
                AppLog.info(TAG, "Reubicación eliminada de BD: $id")
            } catch (e: Exception) {
                AppLog.error(TAG, "Error al eliminar la reubicación local $id", e)
            }
        }
        return resultado
    }
}

class ObtenerReubicacionesPendientesUseCase(
    private val repository: ReubicacionRepository
) {
    suspend operator fun invoke(): List<Reubicacion> = try {
        repository.pendientes()
    } catch (e: Exception) {
        AppLog.error(TAG, "Error al obtener reubicaciones pendientes", e)
        emptyList()
    }
}

class EliminarReubicacionUseCase(
    private val repository: ReubicacionRepository
) {
    suspend operator fun invoke(id: Long): Boolean = try {
        repository.eliminar(id)
    } catch (e: Exception) {
        AppLog.error(TAG, "Error al eliminar la reubicación $id", e)
        false
    }
}
