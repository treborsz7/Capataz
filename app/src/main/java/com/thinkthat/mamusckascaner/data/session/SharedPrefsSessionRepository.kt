package com.thinkthat.mamusckascaner.data.session

import android.content.Context
import com.thinkthat.mamusckascaner.domain.model.Sesion
import com.thinkthat.mamusckascaner.domain.repository.SessionRepository

/**
 * Sesión persistida en SharedPreferences por LoginActivity.
 * Es el único punto de la app que conoce estas claves.
 */
class SharedPrefsSessionRepository(context: Context) : SessionRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun sesionActual(): Sesion = Sesion(
        usuario = prefs.getString(KEY_USUARIO, "").orEmpty(),
        empresa = prefs.getString(KEY_EMPRESA, "").orEmpty(),
        codDeposito = prefs.getString(KEY_DEPOSITO, "").orEmpty()
    )

    override fun codDeposito(): String = prefs.getString(KEY_DEPOSITO, "").orEmpty()

    companion object {
        const val PREFS_NAME = "QRCodeScannerPrefs"
        private const val KEY_USUARIO = "savedUser"
        private const val KEY_EMPRESA = "savedEmpresa"
        private const val KEY_DEPOSITO = "savedDeposito"
    }
}
