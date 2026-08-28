package com.thinkthat.mamusckascaner.data.remote

import org.json.JSONObject

/**
 * Traduce el cuerpo de error de la API al texto que se muestra en pantalla.
 * La API devuelve normalmente {"detail": "..."}; si no se puede parsear se usa
 * el cuerpo crudo. Los saltos de línea se aplanan porque el mensaje se muestra
 * en un componente de una sola línea.
 */
object ApiErrorParser {
    private const val MENSAJE_POR_DEFECTO = "Error desconocido"

    fun parse(errorBody: String?, porDefecto: String = MENSAJE_POR_DEFECTO): String {
        val body = errorBody?.takeIf { it.isNotBlank() } ?: porDefecto
        val detalle = try {
            JSONObject(body).optString("detail", body)
        } catch (e: Exception) {
            body
        }
        return detalle.flatten()
    }

    fun parseException(throwable: Throwable, fallback: String): String =
        (throwable.message ?: fallback).flatten()

    private fun String.flatten(): String = replace("\n", " ").replace("\r", " ")
}
