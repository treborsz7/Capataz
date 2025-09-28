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
    private const val LOG_FILE_NAME = "app_log.txt"
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        writeLog("ERROR", tag, message, throwable)
    }

    fun logInfo(tag: String, message: String) {
        writeLog("INFO", tag, message, null)
    }

    private fun writeLog(level: String, tag: String, message: String, throwable: Throwable?) {
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

        val logFile = File(logsDir, LOG_FILE_NAME)
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
