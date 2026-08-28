package com.thinkthat.mamusckascaner.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thinkthat.mamusckascaner.presentation.estivacion.EstivacionListViewModel
import com.thinkthat.mamusckascaner.presentation.estivacion.EstivacionViewModel
import com.thinkthat.mamusckascaner.presentation.reubicacion.ReubicacionListViewModel
import com.thinkthat.mamusckascaner.presentation.reubicacion.ReubicacionViewModel

/**
 * Fábricas que arman los ViewModels con las dependencias del [ServiceLocator].
 * Es el único lugar de Android que conoce sus constructores.
 */
object ViewModelFactories {

    val estivacion: ViewModelProvider.Factory = factory {
        EstivacionViewModel(
            guardarBorrador = ServiceLocator.guardarBorradorEstivacion,
            enviarEstivacion = ServiceLocator.enviarEstivacion,
            obtenerPendientes = ServiceLocator.obtenerEstivacionesPendientes,
            obtenerUbicaciones = ServiceLocator.obtenerUbicacionesParaEstibar
        )
    }

    val estivacionList: ViewModelProvider.Factory = factory {
        EstivacionListViewModel(
            obtenerPendientes = ServiceLocator.obtenerEstivacionesPendientes,
            eliminarEstivacion = ServiceLocator.eliminarEstivacion
        )
    }

    val reubicacion: ViewModelProvider.Factory = factory {
        ReubicacionViewModel(
            guardarBorrador = ServiceLocator.guardarBorradorReubicacion,
            enviarReubicacion = ServiceLocator.enviarReubicacion,
            obtenerPendientes = ServiceLocator.obtenerReubicacionesPendientes,
            session = ServiceLocator.session
        )
    }

    val reubicacionList: ViewModelProvider.Factory = factory {
        ReubicacionListViewModel(
            obtenerPendientes = ServiceLocator.obtenerReubicacionesPendientes,
            eliminarReubicacion = ServiceLocator.eliminarReubicacion
        )
    }

    private fun factory(builder: () -> ViewModel): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = builder() as T
        }
}
