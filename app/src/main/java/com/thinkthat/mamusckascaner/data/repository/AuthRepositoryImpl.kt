package com.thinkthat.mamusckascaner.data.repository

import com.thinkthat.mamusckascaner.core.AppError
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.data.remote.ApiErrorParser
import com.thinkthat.mamusckascaner.domain.repository.AuthRepository
import com.thinkthat.mamusckascaner.service.Services.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AuthRepository"

/**
 * El endpoint Login/Plano devuelve el token como cuerpo crudo, no como JSON.
 */
class AuthRepositoryImpl(
    private val api: ApiService
) : AuthRepository {

    override suspend fun login(usuario: String, contrasena: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.loginPlano(nombreUsuario = usuario, pass = contrasena).execute()
                val body = response.body()?.string()

                if (response.isSuccessful && body != null) {
                    AppResult.Success(body)
                } else {
                    val errorBody = response.errorBody()?.string()
                    AppLog.error(
                        TAG,
                        "LoginPlano falló: code=${response.code()} " +
                            "message=${response.message()} body=$errorBody"
                    )
                    AppResult.Failure(
                        AppError(ApiErrorParser.parse(errorBody), response.code())
                    )
                }
            } catch (e: Exception) {
                AppLog.error(TAG, "LoginPlano onFailure: ${e.message}", e)
                AppResult.Failure(
                    AppError(
                        ApiErrorParser.parseException(
                            e,
                            "No se pudo iniciar sesión por un problema de conexión."
                        ),
                        cause = e
                    )
                )
            }
        }
}
