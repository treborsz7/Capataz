package com.thinkthat.mamusckascaner.core

/**
 * Empresa, depósito y servidor con los que opera la app. Están fijos por build:
 * cambiar acá alterna entre el entorno de prueba y el de producción.
 */
object AppConfig {
    // Prueba
    //const val EMPRESA = "31"
    //const val DEPOSITO = "3B"

    // Producción (productos terminados)
     const val EMPRESA = "3"
     const val DEPOSITO = "4B"

    /**
     * Servidor de las APIs. Incluye esquema, IP y puerto, y termina en "/"
     * porque Retrofit lo exige y el resto del código concatena la ruta directamente
     * (por ejemplo "${BASE_URL}Login/Plano").
     */
    const val BASE_URL = "http://191.235.41.83:18001/"
}
