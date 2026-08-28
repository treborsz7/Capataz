package com.thinkthat.mamusckascaner.data.session

import android.content.Context
import com.thinkthat.mamusckascaner.domain.model.Credenciales
import com.thinkthat.mamusckascaner.domain.model.Sesion
import com.thinkthat.mamusckascaner.domain.repository.SessionRepository

/**
 * Sesión persistida en SharedPreferences.
 * Es el único punto de la app que conoce estas claves.
 */
class SharedPrefsSessionRepository(context: Context) : SessionRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Lectura ---

    override fun sesionActual(): Sesion = Sesion(
        usuario = prefs.getString(KEY_USUARIO, "").orEmpty(),
        empresa = prefs.getString(KEY_EMPRESA, "").orEmpty(),
        codDeposito = prefs.getString(KEY_DEPOSITO, "").orEmpty()
    )

    override fun codDeposito(): String = prefs.getString(KEY_DEPOSITO, "").orEmpty()

    override fun usuario(): String = prefs.getString(KEY_USUARIO, "").orEmpty()

    override fun ultimoDeposito(): String = prefs.getString(KEY_ULTIMO_DEPOSITO, "").orEmpty()

    override fun credenciales(): Credenciales = Credenciales(
        usuario = prefs.getString(KEY_USUARIO, "").orEmpty(),
        contrasena = prefs.getString(KEY_PASS, "").orEmpty(),
        recordar = prefs.getBoolean(KEY_RECORDAR, false)
    )

    // --- Escritura ---

    override fun guardarToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun guardarEmpresaYDeposito(empresa: String, deposito: String) {
        prefs.edit()
            .putString(KEY_EMPRESA, empresa)
            .putString(KEY_DEPOSITO, deposito)
            .apply()
    }

    override fun recordarCredenciales(usuario: String, contrasena: String) {
        prefs.edit()
            .putString(KEY_USUARIO, usuario)
            .putString(KEY_PASS, contrasena)
            .putBoolean(KEY_RECORDAR, true)
            .apply()
    }

    override fun olvidarCredenciales() {
        prefs.edit()
            .remove(KEY_USUARIO)
            .remove(KEY_PASS)
            .putBoolean(KEY_RECORDAR, false)
            .apply()
    }

    override fun cerrarSesion() {
        prefs.edit()
            .remove(KEY_USUARIO)
            .remove(KEY_PASS)
            .remove(KEY_TOKEN)
            .putBoolean(KEY_RECORDAR, false)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "QRCodeScannerPrefs"
        private const val KEY_USUARIO = "savedUser"
        private const val KEY_PASS = "savedPass"
        private const val KEY_RECORDAR = "savedRemember"
        private const val KEY_TOKEN = "token"
        private const val KEY_EMPRESA = "savedEmpresa"
        private const val KEY_DEPOSITO = "savedDeposito"
        private const val KEY_ULTIMO_DEPOSITO = "ultimoDeposito"
    }
}
