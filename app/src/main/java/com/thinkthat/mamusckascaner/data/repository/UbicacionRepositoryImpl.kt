package com.thinkthat.mamusckascaner.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thinkthat.mamusckascaner.core.AppError
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.data.mapper.toDomain
import com.thinkthat.mamusckascaner.data.remote.ApiErrorParser
import com.thinkthat.mamusckascaner.domain.model.Ubicacion
import com.thinkthat.mamusckascaner.domain.repository.UbicacionRepository
import com.thinkthat.mamusckascaner.service.Services.ApiService
import com.thinkthat.mamusckascaner.service.Services.UbicacionResponse
import com.thinkthat.mamusckascaner.service.Services.UbicacionesWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "UbicacionRepository"

/**
 * El endpoint UbicacionesParaEstibar devuelve el cuerpo crudo y, según el caso,
 * un array de ubicaciones o un objeto con la clave "resultado". Se intentan
 * ambas formas antes de dar el parseo por fallido.
 */
class UbicacionRepositoryImpl(
    private val api: ApiService,
    private val gson: Gson = Gson()
) : UbicacionRepository {

    override suspend fun ubicacionesParaEstibar(
        codDeposito: String,
        codArticulo: String?,
        optimizaRecorrido: Boolean
    ): AppResult<List<Ubicacion>> = withContext(Dispatchers.IO) {
        try {
            val response = api.ubicacionesParaEstibar(
                codDeposi = codDeposito,
                codArticu = codArticulo,
                optimizaRecorrido = optimizaRecorrido
            ).execute()

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                AppLog.error(TAG, "Ubicaciones fallidas: code=${response.code()} body=$errorBody")
                return@withContext AppResult.Failure(
                    AppError(ApiErrorParser.parse(errorBody), response.code())
                )
            }

            val rawBody = response.body()?.string().orEmpty()
            AppLog.debug(TAG, "Body: $rawBody")
            AppResult.Success(parseUbicaciones(rawBody))
        } catch (e: Exception) {
            AppLog.error(TAG, "Excepción al obtener ubicaciones: ${e.message}", e)
            AppResult.Failure(
                AppError(
                    ApiErrorParser.parseException(
                        e,
                        "No se pudieron obtener las ubicaciones."
                    ),
                    cause = e
                )
            )
        }
    }

    private fun parseUbicaciones(rawBody: String): List<Ubicacion> {
        if (rawBody.isBlank()) return emptyList()

        val comoArray = runCatching {
            val tipo = object : TypeToken<List<UbicacionResponse>>() {}.type
            gson.fromJson<List<UbicacionResponse>>(rawBody, tipo)
        }.getOrNull()
        if (comoArray != null) return comoArray.map { it.toDomain() }

        val comoWrapper = runCatching {
            gson.fromJson(rawBody, UbicacionesWrapper::class.java)
        }.getOrNull()
        if (comoWrapper != null) return comoWrapper.resultado.map { it.toDomain() }

        AppLog.error(TAG, "No se pudo parsear la respuesta de ubicaciones")
        return emptyList()
    }
}
