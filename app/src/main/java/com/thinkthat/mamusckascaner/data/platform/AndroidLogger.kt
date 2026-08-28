package com.thinkthat.mamusckascaner.data.platform

import android.util.Log
import com.thinkthat.mamusckascaner.core.Logger
import com.thinkthat.mamusckascaner.utils.AppLogger

/**
 * Adaptador de [Logger] al logcat + los archivos de log que ya escribe AppLogger.
 */
object AndroidLogger : Logger {

    override fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun info(tag: String, message: String) {
        Log.i(tag, message)
        AppLogger.logInfo(tag, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
        AppLogger.logError(tag, message, throwable)
    }
}
