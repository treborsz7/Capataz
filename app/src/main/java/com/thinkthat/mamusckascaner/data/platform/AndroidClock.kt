package com.thinkthat.mamusckascaner.data.platform

import com.thinkthat.mamusckascaner.core.Clock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Implementación JVM/Android de [Clock]. */
class AndroidClock : Clock {

    override fun nowIso(): String = isoFormat().format(Date())

    override fun nowLocal(): String = localFormat().format(Date())

    // SimpleDateFormat no es thread-safe: se crea una instancia por llamada.
    private fun isoFormat() =
        SimpleDateFormat(FORMATO_ISO, Locale.getDefault())

    private fun localFormat() =
        SimpleDateFormat(FORMATO_LOCAL, Locale.getDefault())

    private companion object {
        const val FORMATO_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        const val FORMATO_LOCAL = "yyyy-MM-dd HH:mm:ss"
    }
}
