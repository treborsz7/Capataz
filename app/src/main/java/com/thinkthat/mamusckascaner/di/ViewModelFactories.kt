package com.thinkthat.mamusckascaner.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thinkthat.mamusckascaner.presentation.estivacion.EstivacionListViewModel
import com.thinkthat.mamusckascaner.presentation.estivacion.EstivacionViewModel
import com.thinkthat.mamusckascaner.presentation.login.LoginViewModel
import com.thinkthat.mamusckascaner.presentation.main.MainViewModel
import com.thinkthat.mamusckascaner.presentation.ordenes.ListadoOrdenesViewModel
import com.thinkthat.mamusckascaner.presentation.recolectar.RecolectarQrViewModel
import com.thinkthat.mamusckascaner.presentation.recolectar.RecolectarViewModel
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

    val recolectar: ViewModelProvider.Factory = factory {
        RecolectarViewModel(
            obtenerUbicaciones = ServiceLocator.obtenerUbicacionesParaRecolectar,
            restaurarRenglones = ServiceLocator.restaurarRenglones,
            registrarPedido = ServiceLocator.registrarPedido,
            guardarRenglonUseCase = ServiceLocator.guardarRenglon,
            eliminarRenglonUseCase = ServiceLocator.eliminarRenglon,
            enviarRecoleccion = ServiceLocator.enviarRecoleccion
        )
    }

    val recolectarQr: ViewModelProvider.Factory = factory {
        RecolectarQrViewModel(
            obtenerPendientes = ServiceLocator.obtenerPedidosPendientes,
            eliminarPedido = ServiceLocator.eliminarPedido
        )
    }

    val login: ViewModelProvider.Factory = factory {
        LoginViewModel(
            iniciarSesion = ServiceLocator.iniciarSesion,
            autoLogin = ServiceLocator.autoLogin,
            obtenerCredenciales = ServiceLocator.obtenerCredenciales
        )
    }

    val main: ViewModelProvider.Factory = factory {
        MainViewModel(
            session = ServiceLocator.session,
            cerrarSesionUseCase = ServiceLocator.cerrarSesion
        )
    }

    val listadoOrdenes: ViewModelProvider.Factory = factory {
        ListadoOrdenesViewModel(repository = ServiceLocator.ordenesRepository)
    }

    private fun factory(builder: () -> ViewModel): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = builder() as T
        }
}
