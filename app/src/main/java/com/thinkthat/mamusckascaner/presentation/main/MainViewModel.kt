package com.thinkthat.mamusckascaner.presentation.main

import androidx.lifecycle.ViewModel
import com.thinkthat.mamusckascaner.domain.model.Sesion
import com.thinkthat.mamusckascaner.domain.repository.SessionRepository
import com.thinkthat.mamusckascaner.domain.usecase.CerrarSesionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Menú principal: expone la sesión activa y resuelve el cierre de sesión. */
class MainViewModel(
    private val session: SessionRepository,
    private val cerrarSesionUseCase: CerrarSesionUseCase
) : ViewModel() {

    private val _sesion = MutableStateFlow(session.sesionActual())
    val sesion: StateFlow<Sesion> = _sesion.asStateFlow()

    fun cerrarSesion() {
        cerrarSesionUseCase()
        _sesion.value = session.sesionActual()
    }
}
