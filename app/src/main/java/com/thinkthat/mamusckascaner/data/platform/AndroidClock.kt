package com.thinkthat.mamusckascaner.data.platform

import com.thinkthat.mamusckascaner.core.Clock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Implementación JVM/Android de [Clock]. */
class AndroidClock : Clock {

    override fun nowIso(): String = formatear(FORMATO_ISO)

    override fun nowIsoCorto(): String = formatear(FORMATO_ISO_CORTO)

    override fun nowLocal(): String = formatear(FORMATO_LOCAL)

    // SimpleDateFormat no es thread-safe: se crea una instancia por llamada.
    private fun formatear(patron: String) =
        SimpleDateFormat(patron, Locale.getDefault()).format(Date())

    private companion object {
        const val FORMATO_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        const val FORMATO_ISO_CORTO = "yyyy-MM-dd'T'HH:mm:ss"
        const val FORMATO_LOCAL = "yyyy-MM-dd HH:mm:ss"
    }
}
