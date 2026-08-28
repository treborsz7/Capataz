package com.thinkthat.mamusckascaner.domain.usecase

import com.thinkthat.mamusckascaner.core.AppLog
import com.thinkthat.mamusckascaner.core.AppResult
import com.thinkthat.mamusckascaner.core.Clock
import com.thinkthat.mamusckascaner.domain.model.EnvioRecoleccion
import com.thinkthat.mamusckascaner.domain.model.EstadoRegistro
import com.thinkthat.mamusckascaner.domain.model.PedidoRecoleccion
import com.thinkthat.mamusckascaner.domain.model.RenglonEnvio
import com.thinkthat.mamusckascaner.domain.model.RenglonRecoleccion
import com.thinkthat.mamusckascaner.domain.model.UbicacionRecolectar
import com.thinkthat.mamusckascaner.domain.repository.RecolectarRepository
import com.thinkthat.mamusckascaner.domain.repository.SessionRepository

private const val TAG = "RecolectarUseCase"

class ObtenerUbicacionesParaRecolectarUseCase(
    private val repository: RecolectarRepository
) {
    suspend operator fun invoke(
        idPedido: Int,
        optimizaRecorrido: Boolean = false
    ): AppResult<List<UbicacionRecolectar>> =
        repository.ubicacionesParaRecolectar(idPedido, optimizaRecorrido)
}

/**
 * Recupera los renglones guardados del pedido y los reconcilia contra las
 * ubicaciones que devuelve la API ahora:
 *
 * - si la ubicación ya no existe, el renglón local se borra;
 * - si cambió la cantidad requerida, el renglón local se actualiza.
 *
 * Devuelve los renglones ya reconciliados, ordenados por artículo e índice.
 */
class RestaurarRenglonesUseCase(
    private val repository: RecolectarRepository
) {
    suspend operator fun invoke(
        idPedido: Int,
        ubicaciones: List<UbicacionRecolectar>
    ): List<RenglonRecoleccion> {
        if (idPedido <= 0) return emptyList()

        val guardados = try {
            repository.renglonesDelPedido(idPedido)
        } catch (e: Exception) {
            AppLog.error(TAG, "Error al leer los renglones del pedido $idPedido", e)
            return emptyList()
        }
        if (guardados.isEmpty()) return emptyList()

        // Sin ubicaciones de referencia no hay contra qué reconciliar.
        if (ubicaciones.isEmpty()) return guardados

        // Primera ubicación por clave artículo+ubicación, igual que el mapa que
        // se armaba antes en la pantalla.
        val porClave = ubicaciones.associateBy(
            keySelector = { "${it.codArticulo}-${it.nombreUbicacion}" },
            valueTransform = { it }
        )

        val reconciliados = mutableListOf<RenglonRecoleccion>()
        for (renglon in guardados) {
            val ubicacion = porClave["${renglon.codArticulo}-${renglon.ubicacion}"]

            if (ubicacion == null) {
                AppLog.info(
                    TAG,
                    "Reconciliación: se elimina ${renglon.codArticulo} en ${renglon.ubicacion} " +
                        "(la ubicación ya no está en la API)"
                )
                runCatching { repository.eliminarRenglon(renglon.id) }
                continue
            }

            val requeridoActual = ubicacion.requerido
            if (requeridoActual != renglon.cantidad && requeridoActual > 0) {
                AppLog.info(
                    TAG,
                    "Reconciliación: ${renglon.codArticulo} pasa de ${renglon.cantidad} a $requeridoActual"
                )
                val actualizado = renglon.copy(cantidad = requeridoActual)
                runCatching { repository.actualizarRenglon(actualizado) }
                reconciliados += actualizado
            } else {
                reconciliados += renglon
            }
        }
        return reconciliados
    }
}

class ObtenerPedidosPendientesUseCase(
    private val repository: RecolectarRepository
) {
    suspend operator fun invoke(): List<PedidoRecoleccion> = try {
        repository.pedidosPendientes()
    } catch (e: Exception) {
        AppLog.error(TAG, "Error al obtener los pedidos pendientes", e)
        emptyList()
    }
}

/** Descarta un pedido a medio recolectar junto con sus renglones. */
class EliminarPedidoUseCase(
    private val repository: RecolectarRepository
) {
    suspend operator fun invoke(idPedido: Int): Boolean = try {
        repository.eliminarPedidoConRenglones(idPedido)
    } catch (e: Exception) {
        AppLog.error(TAG, "Error al eliminar el pedido $idPedido", e)
        false
    }
}

/**
 * Registra el pedido localmente la primera vez que se guarda una cantidad.
 * El depósito sale del QR y, si el QR no lo trae, del último depósito usado.
 */
class RegistrarPedidoUseCase(
    private val repository: RecolectarRepository,
    private val session: SessionRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(
        idPedido: Int,
        qrDeposito: String?,
        ubicaciones: List<UbicacionRecolectar>
    ) {
        if (idPedido <= 0) return
        try {
            if (repository.pedido(idPedido) != null) return

            repository.guardarPedido(
                PedidoRecoleccion(
                    idPedido = idPedido,
                    codDeposito = qrDeposito.orEmpty().ifEmpty { session.ultimoDeposito() },
                    fechaCreacion = clock.nowIsoCorto(),
                    estado = EstadoRegistro.PENDIENTE,
                    ubicacionesJson = repository.serializarUbicaciones(ubicaciones)
                )
            )
            AppLog.info(TAG, "Pedido $idPedido guardado localmente")
        } catch (e: Exception) {
            AppLog.error(TAG, "Error al guardar el pedido $idPedido", e)
        }
    }
}

/**
 * Guarda (o actualiza) un renglón escaneado. El depósito efectivo es el del QR
 * y, solo si el QR no vino, el último depósito usado.
 */
class GuardarRenglonUseCase(
    private val repository: RecolectarRepository,
    private val session: SessionRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(
        idPedido: Int,
        qrDeposito: String?,
        codArticulo: String,
        nombreArticulo: String,
        cantidadSolicitada: Double,
        indiceScaneo: Int,
        partida: String,
        ubicacion: String,
        cantidad: Double
    ): Long {
        if (idPedido <= 0) return -1L
        return try {
            repository.guardarRenglon(
                RenglonRecoleccion(
                    idPedido = idPedido,
                    codArticulo = codArticulo,
                    nombreArticulo = nombreArticulo,
                    cantidadSolicitada = cantidadSolicitada,
                    ubicacion = ubicacion,
                    partida = partida,
                    cantidad = cantidad,
                    codDeposito = qrDeposito ?: session.ultimoDeposito(),
                    usuario = session.usuario(),
                    fechaHora = clock.nowIsoCorto(),
                    sincronizado = false,
                    indiceScaneo = indiceScaneo
                )
            )
        } catch (e: Exception) {
            AppLog.error(TAG, "Error al guardar el renglón de $codArticulo", e)
            -1L
        }
    }
}

/** Borra el renglón local que corresponde a un artículo y un índice de escaneo. */
class EliminarRenglonUseCase(
    private val repository: RecolectarRepository
) {
    suspend operator fun invoke(idPedido: Int, codArticulo: String, indiceScaneo: Int) {
        if (idPedido <= 0) return
        try {
            val renglon = repository.renglonesDelPedido(idPedido).find {
                it.codArticulo == codArticulo && it.indiceScaneo == indiceScaneo
            }
            if (renglon == null) {
                AppLog.debug(TAG, "Renglón $codArticulo#$indiceScaneo no estaba guardado")
                return
            }
            repository.eliminarRenglon(renglon.id)
            AppLog.debug(TAG, "Renglón eliminado: id=${renglon.id}, índice=$indiceScaneo")
        } catch (e: Exception) {
            AppLog.error(TAG, "Error al eliminar el renglón $codArticulo#$indiceScaneo", e)
        }
    }
}

/**
 * Envía la recolección y, si el envío fue exitoso, marca los renglones y el
 * pedido como sincronizados.
 */
class EnviarRecoleccionUseCase(
    private val repository: RecolectarRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(
        idPedido: Int,
        qrDeposito: String?,
        renglones: List<RenglonEnvio>
    ): AppResult<Unit> {
        val usuario = session.usuario()
        val codDeposito = qrDeposito ?: session.ultimoDeposito()

        AppLog.info(
            TAG,
            "Enviando recolección - pedido: $idPedido, depósito: $codDeposito, " +
                "renglones: ${renglones.size}"
        )

        val resultado = repository.enviarRecoleccion(
            EnvioRecoleccion(
                idPedido = idPedido,
                codDeposito = codDeposito,
                usuario = usuario,
                renglones = renglones
            )
        )

        if (resultado is AppResult.Success && idPedido > 0) {
            try {
                repository.renglonesDelPedido(idPedido).forEach {
                    repository.marcarRenglonSincronizado(it.id)
                }
                repository.actualizarEstadoPedido(idPedido, EstadoRegistro.SINCRONIZADO)
                AppLog.info(TAG, "Renglones del pedido $idPedido marcados como sincronizados")
            } catch (e: Exception) {
                // El envío ya fue exitoso: el fallo de limpieza local no se propaga.
                AppLog.error(TAG, "Error al actualizar el estado local del pedido $idPedido", e)
            }
        }
        return resultado
    }
}

