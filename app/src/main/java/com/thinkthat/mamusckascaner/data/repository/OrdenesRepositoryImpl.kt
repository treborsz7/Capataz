package com.thinkthat.mamusckascaner.data.repository

import com.thinkthat.mamusckascaner.core.AppError
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.data.mapper.toDomain
import com.thinkthat.mamusckascaner.data.remote.ApiErrorParser
import com.thinkthat.mamusckascaner.domain.model.OrdenTrabajoLanzada
import com.thinkthat.mamusckascaner.domain.repository.OrdenesRepository
import com.thinkthat.mamusckascaner.service.Services.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "OrdenesRepository"

class OrdenesRepositoryImpl(
    private val api: ApiService
) : OrdenesRepository {

    override suspend fun ordenesLanzadas(): AppResult<List<OrdenTrabajoLanzada>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.obtenerOrdenesLanzadas().execute()
                if (response.isSuccessful) {
                    AppResult.Success(response.body().orEmpty().map { it.toDomain() })
                } else {
                    val errorBody = response.errorBody()?.string()
                    AppLog.error(
                        TAG,
                        "Error al cargar órdenes: code=${response.code()} " +
                            "message=${response.message()} body=${errorBody ?: "sin cuerpo"}"
                    )
                    AppResult.Failure(
                        AppError(
                            ApiErrorParser.parse(
                                errorBody,
                                "No se pudieron cargar las órdenes. Intenta nuevamente."
                            ),
                            response.code()
                        )
                    )
                }
            } catch (e: Exception) {
                AppLog.error(TAG, "Error de conexión al cargar órdenes: ${e.message}", e)
                AppResult.Failure(
                    AppError(
                        ApiErrorParser.parseException(
                            e,
                            "No se pudieron cargar las órdenes. Verifica tu conexión."
                        ),
                        cause = e
                    )
                )
            }
        }
}
