package com.thinkthat.mamusckascaner.data.repository

import com.thinkthat.mamusckascaner.core.AppError
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.core.Clock
import com.thinkthat.mamusckascaner.data.mapper.toDomain
import com.thinkthat.mamusckascaner.data.mapper.toEntity
import com.thinkthat.mamusckascaner.data.remote.ApiErrorParser
import com.thinkthat.mamusckascaner.database.DatabaseHelper
import com.thinkthat.mamusckascaner.domain.model.Estivacion
import com.thinkthat.mamusckascaner.domain.repository.EstivacionRepository
import com.thinkthat.mamusckascaner.service.Services.ApiService
import com.thinkthat.mamusckascaner.service.Services.EstibarPartida
import com.thinkthat.mamusckascaner.service.Services.EstibarPartidasRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "EstivacionRepository"

/**
 * Implementación Android: SQLite para los borradores y Retrofit para el envío.
 * En iOS esta clase se reemplaza por una equivalente sobre SQLDelight/Ktor;
 * el dominio y los ViewModels no cambian.
 */
class EstivacionRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val api: ApiService,
    private val clock: Clock
) : EstivacionRepository {

    override suspend fun insertar(estivacion: Estivacion): Long = withContext(Dispatchers.IO) {
        dbHelper.insertEstivacion(estivacion.toEntity())
    }

    override suspend fun actualizar(estivacion: Estivacion): Boolean = withContext(Dispatchers.IO) {
        dbHelper.updateEstivacion(estivacion.toEntity()) > 0
    }

    override suspend fun pendientes(): List<Estivacion> = withContext(Dispatchers.IO) {
        dbHelper.getEstivacionesPendientes().map { it.toDomain() }
    }

    override suspend fun eliminar(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteEstivacion(id) > 0
    }

    override suspend fun enviar(estivacion: Estivacion): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            val request = EstibarPartidasRequest(
                partidas = listOf(
                    EstibarPartida(
                        nombreUbicacion = estivacion.ubicacion,
                        numPartida = estivacion.partida
                    )
                ),
                fechaHora = clock.nowIso(),
                codDeposito = estivacion.codDeposito,
                observacion = ""
            )

            try {
                val response = api.estibarPartidas(request).execute()
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    AppLog.info(
                        TAG,
                        "Estivación exitosa - Partida: ${estivacion.partida}, " +
                            "Ubicación: ${estivacion.ubicacion}, Response: $body"
                    )
                    AppResult.Success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string()
                    AppLog.error(
                        TAG,
                        "Error al estibar partida: code=${response.code()}, error=$errorBody, " +
                            "partida=${estivacion.partida}, ubicación=${estivacion.ubicacion}"
                    )
                    AppResult.Failure(
                        AppError(ApiErrorParser.parse(errorBody), response.code())
                    )
                }
            } catch (e: Exception) {
                AppLog.error(
                    TAG,
                    "Excepción al estibar partida: ${e.message}, partida=${estivacion.partida}, " +
                        "ubicación=${estivacion.ubicacion}",
                    e
                )
                AppResult.Failure(
                    AppError(
                        ApiErrorParser.parseException(
                            e,
                            "No se pudo enviar la estivación por un problema de conexión."
                        ),
                        cause = e
                    )
                )
            }
        }
}
