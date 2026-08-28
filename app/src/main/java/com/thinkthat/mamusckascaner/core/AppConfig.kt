package com.thinkthat.mamusckascaner.core

/**
 * Empresa y depósito con los que opera la app. Están fijos por build:
 * cambiar acá alterna entre el entorno de prueba y el de producción.
 */
object AppConfig {
    // Prueba
    const val EMPRESA = "31"
    const val DEPOSITO = "3B"

    // Producción (productos terminados)
    // const val EMPRESA = "3"
    // const val DEPOSITO = "4B"
}
