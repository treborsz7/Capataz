package com.thinkthat.mamusckascaner.core

/**
 * Provee las marcas de tiempo que necesita el dominio, sin atarlo a
 * java.util.Date / SimpleDateFormat.
 */
interface Clock {
    /** Formato que espera la API: yyyy-MM-dd'T'HH:mm:ss.SSS'Z' */
    fun nowIso(): String

    /** ISO sin milisegundos ni zona: yyyy-MM-dd'T'HH:mm:ss */
    fun nowIsoCorto(): String

    /** Formato de persistencia local: yyyy-MM-dd HH:mm:ss */
    fun nowLocal(): String
}
