package com.thinkthat.mamusckascaner

import android.app.Application
import com.thinkthat.mamusckascaner.di.ServiceLocator
import com.thinkthat.mamusckascaner.service.Services.ApiClient
import com.thinkthat.mamusckascaner.utils.AppLogger

/**
 * Inicializa logger, cliente HTTP y grafo de dependencias antes de que se cree
 * cualquier Activity. Antes esto vivía en LoginActivity, lo que dejaba al resto
 * de las pantallas dependiendo de haber pasado por el login en esta ejecución.
 */
class QRScannerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(applicationContext)
        ApiClient.init(applicationContext)
        ServiceLocator.init(applicationContext)
    }
}
