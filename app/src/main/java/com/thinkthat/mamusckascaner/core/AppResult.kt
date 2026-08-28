package com.thinkthat.mamusckascaner.core

/**
 * Resultado de una operación de dominio. Kotlin puro: sin dependencias de Android,
 * por lo que puede moverse tal cual al módulo compartido de Kotlin Multiplatform.
 */
sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Failure(val error: AppError) : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
}

/**
 * Error de dominio. [code] es el código HTTP cuando el error viene de la API.
 */
data class AppError(
    val message: String,
    val code: Int? = null,
    val cause: Throwable? = null
)

inline fun <T> AppResult<T>.onSuccess(block: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) block(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(block: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) block(error)
    return this
}
