package com.thinkthat.mamusckascaner.core

/**
 * Abstracción de logging para que las capas domain/presentation no dependan de
 * android.util.Log ni de escritura de archivos. Cada plataforma instala su
 * implementación vía [AppLog.install].
 */
interface Logger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

private object NoOpLogger : Logger {
    override fun debug(tag: String, message: String) = Unit
    override fun info(tag: String, message: String) = Unit
    override fun error(tag: String, message: String, throwable: Throwable?) = Unit
}

/** Punto de acceso global al logger de la plataforma activa. */
object AppLog : Logger {
    @Volatile
    private var delegate: Logger = NoOpLogger

    fun install(logger: Logger) {
        delegate = logger
    }

    override fun debug(tag: String, message: String) = delegate.debug(tag, message)
    override fun info(tag: String, message: String) = delegate.info(tag, message)
    override fun error(tag: String, message: String, throwable: Throwable?) =
        delegate.error(tag, message, throwable)
}
