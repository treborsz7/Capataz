package com.thinkthat.mamusckascaner.domain.usecase

import com.thinkthat.mamusckascaner.core.AppError
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.core.Clock
import com.thinkthat.mamusckascaner.domain.model.Estivacion
import com.thinkthat.mamusckascaner.domain.repository.EstivacionRepository
import com.thinkthat.mamusckascaner.domain.repository.SessionRepository

private const val TAG = "EstivacionUseCase"

/**
 * Guarda el borrador de la estivación en curso. Inserta si [id] no existe todavía
 * y actualiza en caso contrario; devuelve el id vigente.
 *
 * Reproduce el guardado automático que antes vivía en EstivacionActivity: alcanza
 * con que uno de los dos campos esté cargado.
 */
class GuardarBorradorEstivacionUseCase(
    private val repository: EstivacionRepository,
    private val session: SessionRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(id: Long, partida: String, ubicacion: String): Long {
        if (partida.isBlank() && ubicacion.isBlank()) return id

        val estivacion = Estivacion(
            id = if (id > 0) id else 0,
            partida = partida,
            ubicacion = ubicacion,
            codDeposito = session.codDeposito(),
            fechaCreacion = clock.nowLocal()
        )

        return try {
            if (id > 0) {
                repository.actualizar(estivacion)
                id
            } else {
                repository.insertar(estivacion)
            }
        } catch (e: Exception) {
            AppLog.error(TAG, "Error al guardar el borrador de estivación", e)
            id
        }
    }
}

/**
 * Envía la estivación a la API y, si el envío fue exitoso, borra el borrador local.
 */
class EnviarEstivacionUseCase(
    private val repository: EstivacionRepository,
    private val session: SessionRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(id: Long, partida: String, ubicacion: String): AppResult<Unit> {
        val estivacion = Estivacion(
            id = id,
            partida = partida,
            ubicacion = ubicacion,
            codDeposito = session.codDeposito(),
            fechaCreacion = clock.nowLocal()
        )

        if (!estivacion.esEnviable) {
            return AppResult.Failure(AppError("Faltan datos: se requieren partida y ubicación."))
        }

        AppLog.info(
            TAG,
            "Iniciando estivación - Partida: $partida, Ubicación: $ubicacion, " +
                "Depósito: ${estivacion.codDeposito}"
        )

        val resultado = repository.enviar(estivacion)
        if (resultado is AppResult.Success && id > 0) {
            try {
                repository.eliminar(id)
                AppLog.info(TAG, "Estivación eliminada de BD: $id")
            } catch (e: Exception) {
                // El envío ya fue exitoso: no se propaga el fallo de limpieza local.
                AppLog.error(TAG, "Error al eliminar la estivación local $id", e)
            }
        }
        return resultado
    }
}

class ObtenerEstivacionesPendientesUseCase(
    private val repository: EstivacionRepository
) {
    suspend operator fun invoke(): List<Estivacion> = try {
        repository.pendientes()
    } catch (e: Exception) {
        AppLog.error(TAG, "Error al obtener estivaciones pendientes", e)
        emptyList()
    }
}

class EliminarEstivacionUseCase(
    private val repository: EstivacionRepository
) {
    suspend operator fun invoke(id: Long): Boolean = try {
        repository.eliminar(id)
    } catch (e: Exception) {
        AppLog.error(TAG, "Error al eliminar la estivación $id", e)
        false
    }
}
