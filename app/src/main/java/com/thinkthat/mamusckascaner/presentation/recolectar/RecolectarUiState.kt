package com.thinkthat.mamusckascaner.presentation.recolectar

import com.thinkthat.mamusckascaner.domain.model.EscaneoRenglon
import com.thinkthat.mamusckascaner.domain.model.UbicacionRecolectar

/**
 * Estado de la pantalla de recolección.
 *
 * Los tres mapas indexados por código de artículo son el reflejo en memoria de
 * lo que se persiste en la base: [escaneos] guarda partida y ubicación de cada
 * escaneo, [cantidades] el texto que el operario escribió, y
 * [cantidadesGuardadas] cuáles ya se confirmaron. El índice de la lista de
 * escaneos es el mismo `indiceScaneo` de la base.
 */
data class RecolectarUiState(
    val idPedido: Int = -1,
    val qrDeposito: String? = null,
    val ubicaciones: List<UbicacionRecolectar> = emptyList(),
    val isLoadingUbicaciones: Boolean = false,
    val errorUbicaciones: String? = null,
    val escaneos: Map<String, List<EscaneoRenglon>> = emptyMap(),
    val cantidades: Map<String, Map<Int, String>> = emptyMap(),
    val cantidadesGuardadas: Map<String, Map<Int, Boolean>> = emptyMap(),
    val isEnviando: Boolean = false,
    val errorEnvio: String? = null,
    val envioExitoso: Boolean = false
) {
    val hayUbicaciones: Boolean
        get() = ubicaciones.isNotEmpty()

    /** Las ubicaciones de un artículo, en el orden en que las devolvió la API. */
    fun ubicacionesDe(codArticulo: String): List<UbicacionRecolectar> =
        ubicaciones.filter { it.codArticulo == codArticulo }

    fun escaneosDe(codArticulo: String): List<EscaneoRenglon> =
        escaneos[codArticulo].orEmpty()

    fun cantidadDe(codArticulo: String, indice: Int): String =
        cantidades[codArticulo]?.get(indice).orEmpty()

    fun cantidadGuardada(codArticulo: String, indice: Int): Boolean =
        cantidadesGuardadas[codArticulo]?.get(indice) ?: false
}
