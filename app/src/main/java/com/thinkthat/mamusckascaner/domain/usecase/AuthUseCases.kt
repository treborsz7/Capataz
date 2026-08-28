package com.thinkthat.mamusckascaner.domain.usecase

import com.thinkthat.mamusckascaner.core.AppConfig
import com.thinkthat.mamusckascaner.core.AppError
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.domain.model.Credenciales
import com.thinkthat.mamusckascaner.domain.repository.AuthRepository
import com.thinkthat.mamusckascaner.domain.repository.SessionRepository

private const val TAG = "AuthUseCase"

/**
 * Inicia sesión y deja la sesión lista: token guardado, empresa y depósito
 * fijados, y credenciales recordadas u olvidadas según [recordar].
 */
class IniciarSesionUseCase(
    private val authRepository: AuthRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(
        usuario: String,
        contrasena: String,
        recordar: Boolean
    ): AppResult<Unit> {
        if (usuario.isBlank()) {
            return AppResult.Failure(AppError("El usuario no puede estar vacío."))
        }

        return when (val resultado = authRepository.login(usuario, contrasena)) {
            is AppResult.Success -> {
                session.guardarToken(resultado.data)
                session.guardarEmpresaYDeposito(AppConfig.EMPRESA, AppConfig.DEPOSITO)
                if (recordar) {
                    session.recordarCredenciales(usuario, contrasena)
                } else {
                    session.olvidarCredenciales()
                }
                AppLog.info(TAG, "Login exitoso para $usuario")
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> resultado
        }
    }
}

/**
 * Intenta entrar con las credenciales recordadas. Devuelve false cuando no hay
 * credenciales o cuando el login falla: en ambos casos hay que mostrar el formulario.
 */
class AutoLoginUseCase(
    private val authRepository: AuthRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(): Boolean {
        // Empresa y depósito son fijos: se dejan escritos aunque no haya auto-login,
        // porque el resto de las pantallas los leen desde la sesión.
        session.guardarEmpresaYDeposito(AppConfig.EMPRESA, AppConfig.DEPOSITO)

        val credenciales = session.credenciales()
        if (!credenciales.permitenAutoLogin) return false

        return when (
            val resultado = authRepository.login(credenciales.usuario, credenciales.contrasena)
        ) {
            is AppResult.Success -> {
                session.guardarToken(resultado.data)
                true
            }
            is AppResult.Failure -> {
                AppLog.error(TAG, "Auto-login falló: ${resultado.error.message}")
                false
            }
        }
    }
}

class ObtenerCredencialesUseCase(
    private val session: SessionRepository
) {
    operator fun invoke(): Credenciales = session.credenciales()
}

/** Cierra sesión. Empresa y depósito se mantienen porque son fijos por build. */
class CerrarSesionUseCase(
    private val session: SessionRepository
) {
    operator fun invoke() {
        session.cerrarSesion()
        AppLog.info(TAG, "Sesión cerrada")
    }
}
