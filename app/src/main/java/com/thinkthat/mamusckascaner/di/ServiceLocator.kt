package com.thinkthat.mamusckascaner.di

import android.content.Context
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.Clock
import com.thinkthat.mamusckascaner.data.platform.AndroidClock
import com.thinkthat.mamusckascaner.data.platform.AndroidLogger
import com.thinkthat.mamusckascaner.data.repository.AuthRepositoryImpl
import com.thinkthat.mamusckascaner.data.repository.EstivacionRepositoryImpl
import com.thinkthat.mamusckascaner.data.repository.OrdenesRepositoryImpl
import com.thinkthat.mamusckascaner.data.repository.RecolectarRepositoryImpl
import com.thinkthat.mamusckascaner.data.repository.ReubicacionRepositoryImpl
import com.thinkthat.mamusckascaner.data.repository.UbicacionRepositoryImpl
import com.thinkthat.mamusckascaner.data.session.SharedPrefsSessionRepository
import com.thinkthat.mamusckascaner.database.DatabaseHelper
import com.thinkthat.mamusckascaner.domain.repository.AuthRepository
import com.thinkthat.mamusckascaner.domain.repository.EstivacionRepository
import com.thinkthat.mamusckascaner.domain.repository.OrdenesRepository
import com.thinkthat.mamusckascaner.domain.repository.RecolectarRepository
import com.thinkthat.mamusckascaner.domain.repository.ReubicacionRepository
import com.thinkthat.mamusckascaner.domain.repository.SessionRepository
import com.thinkthat.mamusckascaner.domain.repository.UbicacionRepository
import com.thinkthat.mamusckascaner.domain.usecase.EliminarEstivacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.EliminarReubicacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.AutoLoginUseCase
import com.thinkthat.mamusckascaner.domain.usecase.CerrarSesionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.EliminarPedidoUseCase
import com.thinkthat.mamusckascaner.domain.usecase.EliminarRenglonUseCase
import com.thinkthat.mamusckascaner.domain.usecase.EnviarEstivacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.EnviarRecoleccionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.EnviarReubicacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.GuardarBorradorEstivacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.GuardarBorradorReubicacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.GuardarRenglonUseCase
import com.thinkthat.mamusckascaner.domain.usecase.IniciarSesionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerCredencialesUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerEstivacionesPendientesUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerPedidosPendientesUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerReubicacionesPendientesUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerUbicacionesParaEstibarUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerUbicacionesParaRecolectarUseCase
import com.thinkthat.mamusckascaner.domain.usecase.RegistrarPedidoUseCase
import com.thinkthat.mamusckascaner.domain.usecase.RestaurarRenglonesUseCase
import com.thinkthat.mamusckascaner.service.Services.ApiClient

/**
 * Cableado de dependencias sin librería de DI: cada capa recibe sus
 * colaboradores por constructor, así que en iOS alcanza con escribir el
 * equivalente de este archivo apuntando a las implementaciones nativas.
 */
object ServiceLocator {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        AppLog.install(AndroidLogger)
    }

    // --- Infraestructura ---

    val clock: Clock by lazy { AndroidClock() }

    private val dbHelper: DatabaseHelper by lazy { DatabaseHelper(appContext) }

    // --- Repositorios ---

    val session: SessionRepository by lazy { SharedPrefsSessionRepository(appContext) }

    val estivacionRepository: EstivacionRepository by lazy {
        EstivacionRepositoryImpl(dbHelper, ApiClient.apiService, clock)
    }

    val reubicacionRepository: ReubicacionRepository by lazy {
        ReubicacionRepositoryImpl(dbHelper, ApiClient.apiService, clock)
    }

    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(ApiClient.apiService) }

    val ubicacionRepository: UbicacionRepository by lazy {
        UbicacionRepositoryImpl(ApiClient.apiService)
    }

    val ordenesRepository: OrdenesRepository by lazy { OrdenesRepositoryImpl(ApiClient.apiService) }

    val recolectarRepository: RecolectarRepository by lazy {
        RecolectarRepositoryImpl(dbHelper, ApiClient.apiService, clock)
    }

    // --- Casos de uso: estivación ---

    val guardarBorradorEstivacion by lazy {
        GuardarBorradorEstivacionUseCase(estivacionRepository, session, clock)
    }
    val enviarEstivacion by lazy {
        EnviarEstivacionUseCase(estivacionRepository, session, clock)
    }
    val obtenerEstivacionesPendientes by lazy {
        ObtenerEstivacionesPendientesUseCase(estivacionRepository)
    }
    val eliminarEstivacion by lazy { EliminarEstivacionUseCase(estivacionRepository) }
    val obtenerUbicacionesParaEstibar by lazy {
        ObtenerUbicacionesParaEstibarUseCase(ubicacionRepository, session)
    }

    // --- Casos de uso: reubicación ---

    val guardarBorradorReubicacion by lazy {
        GuardarBorradorReubicacionUseCase(reubicacionRepository, session, clock)
    }
    val enviarReubicacion by lazy {
        EnviarReubicacionUseCase(reubicacionRepository, session, clock)
    }
    val obtenerReubicacionesPendientes by lazy {
        ObtenerReubicacionesPendientesUseCase(reubicacionRepository)
    }
    val eliminarReubicacion by lazy { EliminarReubicacionUseCase(reubicacionRepository) }

    // --- Casos de uso: recolección ---

    val obtenerUbicacionesParaRecolectar by lazy {
        ObtenerUbicacionesParaRecolectarUseCase(recolectarRepository)
    }
    val restaurarRenglones by lazy { RestaurarRenglonesUseCase(recolectarRepository) }
    val registrarPedido by lazy { RegistrarPedidoUseCase(recolectarRepository, session, clock) }
    val guardarRenglon by lazy { GuardarRenglonUseCase(recolectarRepository, session, clock) }
    val eliminarRenglon by lazy { EliminarRenglonUseCase(recolectarRepository) }
    val enviarRecoleccion by lazy { EnviarRecoleccionUseCase(recolectarRepository, session) }
    val obtenerPedidosPendientes by lazy { ObtenerPedidosPendientesUseCase(recolectarRepository) }
    val eliminarPedido by lazy { EliminarPedidoUseCase(recolectarRepository) }

    // --- Casos de uso: sesión ---

    val iniciarSesion by lazy { IniciarSesionUseCase(authRepository, session) }
    val autoLogin by lazy { AutoLoginUseCase(authRepository, session) }
    val obtenerCredenciales by lazy { ObtenerCredencialesUseCase(session) }
    val cerrarSesion by lazy { CerrarSesionUseCase(session) }
}
