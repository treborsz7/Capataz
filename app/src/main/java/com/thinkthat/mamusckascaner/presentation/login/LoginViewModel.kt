package com.thinkthat.mamusckascaner.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.domain.usecase.AutoLoginUseCase
import com.thinkthat.mamusckascaner.domain.usecase.IniciarSesionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerCredencialesUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Qué se muestra mientras se resuelve el arranque. */
enum class LoginPantalla { AUTENTICANDO, FORMULARIO }

data class LoginUiState(
    val pantalla: LoginPantalla = LoginPantalla.AUTENTICANDO,
    val usuario: String = "",
    val contrasena: String = "",
    val recordar: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val puedeIniciarSesion: Boolean
        get() = !isLoading && usuario.isNotBlank()
}

sealed interface LoginEvent {
    data object LoginExitoso : LoginEvent
}

/**
 * Arranque de la app: intenta el auto-login con las credenciales recordadas y,
 * si no alcanza, expone el formulario.
 */
class LoginViewModel(
    private val iniciarSesion: IniciarSesionUseCase,
    private val autoLogin: AutoLoginUseCase,
    private val obtenerCredenciales: ObtenerCredencialesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    private var iniciado = false

    fun iniciar() {
        if (iniciado) return
        iniciado = true

        val credenciales = obtenerCredenciales()
        _state.update {
            it.copy(
                usuario = credenciales.usuario,
                contrasena = credenciales.contrasena,
                recordar = credenciales.recordar
            )
        }

        viewModelScope.launch {
            if (autoLogin()) {
                _events.emit(LoginEvent.LoginExitoso)
            } else {
                _state.update { it.copy(pantalla = LoginPantalla.FORMULARIO) }
            }
        }
    }

    fun onUsuarioChange(valor: String) {
        _state.update { it.copy(usuario = valor, error = null) }
    }

    fun onContrasenaChange(valor: String) {
        _state.update { it.copy(contrasena = valor, error = null) }
    }

    fun onRecordarChange(valor: Boolean) {
        _state.update { it.copy(recordar = valor) }
    }

    fun login() {
        val actual = _state.value
        if (actual.isLoading) return

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val resultado = iniciarSesion(actual.usuario, actual.contrasena, actual.recordar)
            when (resultado) {
                is AppResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.emit(LoginEvent.LoginExitoso)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = resultado.error.message)
                }
            }
        }
    }

    fun limpiarError() {
        _state.update { it.copy(error = null) }
    }
}
