package com.thinkthat.mamusckascaner.di

import android.content.Context
import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.Clock
import com.thinkthat.mamusckascaner.data.platform.AndroidClock
import com.thinkthat.mamusckascaner.data.platform.AndroidLogger
import com.thinkthat.mamusckascaner.data.repository.EstivacionRepositoryImpl
import com.thinkthat.mamusckascaner.data.repository.ReubicacionRepositoryImpl
import com.thinkthat.mamusckascaner.data.repository.UbicacionRepositoryImpl
import com.thinkthat.mamusckascaner.data.session.SharedPrefsSessionRepository
import com.thinkthat.mamusckascaner.database.DatabaseHelper
import com.thinkthat.mamusckascaner.domain.repository.EstivacionRepository
import com.thinkthat.mamusckascaner.domain.repository.ReubicacionRepository
import com.thinkthat.mamusckascaner.domain.repository.SessionRepository
import com.thinkthat.mamusckascaner.domain.repository.UbicacionRepository
import com.thinkthat.mamusckascaner.domain.usecase.EliminarEstivacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.EliminarReubicacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.EnviarEstivacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.EnviarReubicacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.GuardarBorradorEstivacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.GuardarBorradorReubicacionUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerEstivacionesPendientesUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerReubicacionesPendientesUseCase
import com.thinkthat.mamusckascaner.domain.usecase.ObtenerUbicacionesParaEstibarUseCase
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

    val ubicacionRepository: UbicacionRepository by lazy {
        UbicacionRepositoryImpl(ApiClient.apiService)
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
}
