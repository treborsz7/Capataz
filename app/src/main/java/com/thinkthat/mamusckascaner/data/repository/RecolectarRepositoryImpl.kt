package com.thinkthat.mamusckascaner.data.repository

import com.google.gson.Gson
import com.thinkthat.mamusckascaner.core.AppError
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.core.Clock
import com.thinkthat.mamusckascaner.data.mapper.toDomain
import com.thinkthat.mamusckascaner.data.mapper.toEntity
import com.thinkthat.mamusckascaner.data.mapper.toUbicacionesRecolectar
import com.thinkthat.mamusckascaner.data.remote.ApiErrorParser
import com.thinkthat.mamusckascaner.database.DatabaseHelper
import com.thinkthat.mamusckascaner.domain.model.EnvioRecoleccion
import com.thinkthat.mamusckascaner.domain.model.EstadoRegistro
import com.thinkthat.mamusckascaner.domain.model.PedidoRecoleccion
import com.thinkthat.mamusckascaner.domain.model.RenglonRecoleccion
import com.thinkthat.mamusckascaner.domain.model.UbicacionRecolectar
import com.thinkthat.mamusckascaner.domain.repository.RecolectarRepository
import com.thinkthat.mamusckascaner.service.Services.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "RecolectarRepository"

class RecolectarRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val api: ApiService,
    private val clock: Clock,
    private val gson: Gson = Gson()
) : RecolectarRepository {

    override suspend fun ubicacionesParaRecolectar(
        idPedido: Int,
        optimizaRecorrido: Boolean
    ): AppResult<List<UbicacionRecolectar>> = withContext(Dispatchers.IO) {
        if (idPedido <= 0) {
            return@withContext AppResult.Failure(AppError("Pedido inválido: $idPedido"))
        }

        try {
            val response = api.UbicacionesParaRecolectar(
                idPed = idPedido,
                optimizaRecorrido = optimizaRecorrido
            ).execute()

            if (response.isSuccessful) {
                val ubicaciones = response.body()?.resultado.orEmpty().toUbicacionesRecolectar()
                AppLog.debug(TAG, "Ubicaciones OK. Filas: ${ubicaciones.size}")
                AppResult.Success(ubicaciones)
            } else {
                val code = response.code()
                val errorBody = response.errorBody()?.string()
                AppLog.error(
                    TAG,
                    "Ubicaciones error: code=$code message=${response.message()} " +
                        "body=${errorBody ?: "sin cuerpo"}"
                )
                // El backend responde 404 cuando el pedido no tiene ubicaciones:
                // ahí el mensaje por defecto es distinto.
                val porDefecto = if (code == 404 || response.message().contains("Not Found", true)) {
                    "No se encontraron ubicaciones para el pedido."
                } else {
                    "No se pudieron cargar las ubicaciones. Intenta nuevamente."
                }
                AppResult.Failure(
                    AppError(ApiErrorParser.parse(errorBody, porDefecto), code)
                )
            }
        } catch (e: Exception) {
            AppLog.error(TAG, "Fallo ubicaciones: ${e.message}", e)
            AppResult.Failure(
                AppError(
                    ApiErrorParser.parseException(
                        e,
                        "No se pudieron cargar las ubicaciones. Verifica tu conexión."
                    ),
                    cause = e
                )
            )
        }
    }

    // --- Renglones locales ---

    override suspend fun renglonesDelPedido(idPedido: Int): List<RenglonRecoleccion> =
        withContext(Dispatchers.IO) {
            dbHelper.getRecoleccionesByPedido(idPedido).map { it.toDomain() }
        }

    override suspend fun guardarRenglon(renglon: RenglonRecoleccion): Long =
        withContext(Dispatchers.IO) {
            dbHelper.insertOrUpdateRecoleccion(renglon.toEntity())
        }

    override suspend fun actualizarRenglon(renglon: RenglonRecoleccion): Boolean =
        withContext(Dispatchers.IO) {
            dbHelper.updateRecoleccion(renglon.toEntity()) > 0
        }

    override suspend fun eliminarRenglon(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteRecoleccion(id) > 0
    }

    override suspend fun marcarRenglonSincronizado(id: Long): Boolean =
        withContext(Dispatchers.IO) {
            dbHelper.marcarComoSincronizado(id) > 0
        }

    // --- Pedido local ---

    override suspend fun pedido(idPedido: Int): PedidoRecoleccion? = withContext(Dispatchers.IO) {
        dbHelper.getPedidoByIdPedido(idPedido)?.toDomain()
    }

    override suspend fun pedidosPendientes(): List<PedidoRecoleccion> =
        withContext(Dispatchers.IO) {
            dbHelper.getAllPedidos()
                .filter { it.estado != EstadoRegistro.SINCRONIZADO }
                .map { it.toDomain() }
        }

    override suspend fun eliminarPedidoConRenglones(idPedido: Int): Boolean =
        withContext(Dispatchers.IO) {
            dbHelper.deleteRecoleccionesByPedido(idPedido)
            dbHelper.deletePedido(idPedido) > 0
        }

    override suspend fun guardarPedido(pedido: PedidoRecoleccion): Long =
        withContext(Dispatchers.IO) {
            dbHelper.insertOrUpdatePedido(pedido.toEntity())
        }

    override suspend fun actualizarEstadoPedido(idPedido: Int, estado: String): Boolean =
        withContext(Dispatchers.IO) {
            dbHelper.updatePedidoEstado(idPedido, estado) > 0
        }

    override fun serializarUbicaciones(ubicaciones: List<UbicacionRecolectar>): String =
        gson.toJson(ubicaciones)

    // --- Envío ---

    override suspend fun enviarRecoleccion(envio: EnvioRecoleccion): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            val json = construirPayload(envio)
            AppLog.debug(TAG, "JSON de recolección: $json")

            try {
                val body = json.toRequestBody(JSON_MEDIA_TYPE)
                val response = api.recolectarPedido(body).execute()
                AppLog.debug(
                    TAG,
                    "Respuesta recolección - código: ${response.code()}, " +
                        "exitoso: ${response.isSuccessful}"
                )

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: "Sin contenido"
                    AppLog.info(TAG, "Recolección enviada correctamente: $responseBody")
                    AppResult.Success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string()
                    AppLog.error(TAG, "Error en respuesta: code=${response.code()} body=$errorBody")
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
                            "No se pudo enviar la recolección por un problema de conexión."
                        ),
                        cause = e
                    )
                )
            }
        }

    private fun construirPayload(envio: EnvioRecoleccion): String {
        val renglones = JSONArray()
        envio.renglones.forEach { renglon ->
            renglones.put(
                JSONObject().apply {
                    put("nombreUbi", renglon.nombreUbicacion)
                    put("cantidad", renglon.cantidad)
                    put("codArticulo", renglon.codArticulo)
                    put("codDeposito", envio.codDeposito)
                    put("idEtiqueta", renglon.codArticulo)
                    put("numPartida", renglon.numPartida)
                    put("fyH", clock.nowIso())
                    put("userData", envio.usuario)
                }
            )
        }

        return JSONObject().apply {
            put("idPedido", envio.idPedido)
            put("renglones", renglones)
            put("codDeposito", envio.codDeposito)
            put("fechaHora", clock.nowIso())
            put("observacion", envio.usuario)
            put("userData", envio.usuario)
        }.toString()
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()
    }
}
