package com.thinkthat.mamusckascaner.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val LOG_DIR = "logs"
    const val LOG_FILE_INFO = "app_log_info.txt"
    const val LOG_FILE_ERROR = "app_log_error.txt"
    const val LOG_FILE_EXCEPTION = "app_log_exception.txt"
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        writeLog("ERROR", tag, message, throwable, LOG_FILE_ERROR)
    }

    fun logException(tag: String, message: String, throwable: Throwable) {
        writeLog("EXCEPTION", tag, message, throwable, LOG_FILE_EXCEPTION)
    }

    fun logInfo(tag: String, message: String) {
        writeLog("INFO", tag, message, null, LOG_FILE_INFO)
    }
    
    /**
     * Verifica si existen logs de error o excepción
     */
    fun hasErrorLogs(context: Context): Boolean {
        val logsDir = File(context.getExternalFilesDir(null), LOG_DIR)
        val errorFile = File(logsDir, LOG_FILE_ERROR)
        val exceptionFile = File(logsDir, LOG_FILE_EXCEPTION)
        return (errorFile.exists() && errorFile.length() > 0) || 
               (exceptionFile.exists() && exceptionFile.length() > 0)
    }
    
    /**
     * Obtiene todos los archivos de log
     */
    fun getAllLogFiles(context: Context): List<File> {
        val logsDir = File(context.getExternalFilesDir(null), LOG_DIR)
        if (!logsDir.exists()) return emptyList()
        
        return listOf(
            File(logsDir, LOG_FILE_INFO),
            File(logsDir, LOG_FILE_ERROR),
            File(logsDir, LOG_FILE_EXCEPTION)
        ).filter { it.exists() && it.length() > 0 }
    }
    
    /**
     * Elimina todos los archivos de log
     */
    fun deleteAllLogs(context: Context): Boolean {
        val logsDir = File(context.getExternalFilesDir(null), LOG_DIR)
        if (!logsDir.exists()) return true
        
        var allDeleted = true
        logsDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith("app_log_")) {
                if (!file.delete()) {
                    allDeleted = false
                }
            }
        }
        return allDeleted
    }

    private fun writeLog(level: String, tag: String, message: String, throwable: Throwable?, fileName: String) {
        val context = appContext
        if (context == null) {
            Log.w(tag, "AppLogger not initialized. Message: $message")
            return
        }

        val logsDir = File(context.getExternalFilesDir(null), LOG_DIR)
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            Log.e(tag, "Unable to create logs directory: ${logsDir.absolutePath}")
            return
        }

        val logFile = File(logsDir, fileName)
        val timestamp = dateFormatter.format(Date())
        val builder = StringBuilder()
        builder.append("$timestamp [$level] $tag: $message")
        if (throwable != null) {
            builder.append('\n')
            builder.append(throwable.stackTraceToString())
        }
        builder.append('\n')

        synchronized(this) {
            try {
                FileWriter(logFile, true).use { writer ->
                    writer.append(builder.toString())
                }
            } catch (ioe: IOException) {
                Log.e(tag, "Failed to write log to file", ioe)
            }
        }
    }
}
