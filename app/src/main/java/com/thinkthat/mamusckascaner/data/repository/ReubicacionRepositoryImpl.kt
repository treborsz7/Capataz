package com.thinkthat.mamusckascaner.data.repository

import com.thinkthat.mamusckascaner.core.AppError
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.core.Clock
import com.thinkthat.mamusckascaner.data.mapper.toDomain
import com.thinkthat.mamusckascaner.data.mapper.toEntity
import com.thinkthat.mamusckascaner.data.remote.ApiErrorParser
import com.thinkthat.mamusckascaner.database.DatabaseHelper
import com.thinkthat.mamusckascaner.domain.model.Reubicacion
import com.thinkthat.mamusckascaner.domain.repository.ReubicacionRepository
import com.thinkthat.mamusckascaner.service.Services.ApiService
import com.thinkthat.mamusckascaner.service.Services.ReubicarPartida
import com.thinkthat.mamusckascaner.service.Services.ReubicarPartidasRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ReubicacionRepository"

class ReubicacionRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val api: ApiService,
    private val clock: Clock
) : ReubicacionRepository {

    override suspend fun insertar(reubicacion: Reubicacion): Long = withContext(Dispatchers.IO) {
        dbHelper.insertReubicacion(reubicacion.toEntity())
    }

    override suspend fun actualizar(reubicacion: Reubicacion): Boolean =
        withContext(Dispatchers.IO) {
            dbHelper.updateReubicacion(reubicacion.toEntity()) > 0
        }

    override suspend fun pendientes(): List<Reubicacion> = withContext(Dispatchers.IO) {
        dbHelper.getReubicacionesPendientes().map { it.toDomain() }
    }

    override suspend fun eliminar(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteReubicacion(id) > 0
    }

    override suspend fun enviar(reubicacion: Reubicacion): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            val request = ReubicarPartidasRequest(
                reubicaciones = listOf(
                    ReubicarPartida(
                        nombreUbiOrigen = reubicacion.ubicacionOrigen,
                        nombreUbiDestino = reubicacion.ubicacionDestino,
                        numPartida = reubicacion.partida
                    )
                ),
                fechaHora = clock.nowIso(),
                codDeposito = reubicacion.codDeposito,
                observacion = "",
                reubicacion = 0
            )

            try {
                AppLog.info(TAG, "Enviando request a reubicarPartidas...")
                val response = api.reubicarPartidas(request).execute()
                AppLog.info(
                    TAG,
                    "Respuesta recibida - Código: ${response.code()}, Exitoso: ${response.isSuccessful}"
                )

                if (response.isSuccessful) {
                    val body = response.body()?.string() ?: "Sin contenido"
                    AppLog.info(TAG, "Respuesta exitosa: $body")
                    AppLog.info(
                        TAG,
                        "Reubicación completada exitosamente - Partida: ${reubicacion.partida}"
                    )
                    AppResult.Success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string()
                    AppLog.error(
                        TAG,
                        "Error en respuesta: code=${response.code()} body=$errorBody"
                    )
                    AppResult.Failure(
                        AppError(ApiErrorParser.parse(errorBody), response.code())
                    )
                }
            } catch (e: Exception) {
                AppLog.error(TAG, "Excepción durante el envío: ${e.message}", e)
                AppResult.Failure(
                    AppError(
                        ApiErrorParser.parseException(
                            e,
                            "No se pudo reubicar la partida por un problema de conexión."
                        ),
                        cause = e
                    )
                )
            }
        }
}
