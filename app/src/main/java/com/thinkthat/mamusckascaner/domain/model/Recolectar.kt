package com.thinkthat.mamusckascaner.domain.model

/**
 * Una fila del listado a recolectar: la combinación de una ubicación del depósito
 * con uno de los artículos que hay en ella. La API devuelve ubicaciones con sus
 * artículos anidados; acá vienen ya aplanadas, que es como las consume la pantalla.
 */
data class UbicacionRecolectar(
    val numeroUbicacion: String,
    val nombreUbicacion: String,
    val descripcionArticulo: String,
    val codArticulo: String,
    val requerido: Double,
    val saldoDisponible: Double,
    val nroPartida: String
)

/** Un escaneo cargado por el operario para un artículo. */
data class EscaneoRenglon(
    val partida: String = "",
    val ubicacion: String = ""
)

/**
 * Un renglón de recolección persistido localmente. [indiceScaneo] distingue los
 * múltiples escaneos de un mismo artículo (cuando se recolecta desde varias
 * ubicaciones para cubrir la cantidad requerida).
 */
data class RenglonRecoleccion(
    val id: Long = 0,
    val idPedido: Int,
    val codArticulo: String,
    val nombreArticulo: String,
    val cantidadSolicitada: Double,
    val ubicacion: String,
    val partida: String,
    val cantidad: Double,
    val codDeposito: String,
    val usuario: String,
    val fechaHora: String,
    val sincronizado: Boolean = false,
    val indiceScaneo: Int = 0
)

/** Pedido en curso guardado localmente. */
data class PedidoRecoleccion(
    val id: Long = 0,
    val idPedido: Int,
    val codDeposito: String,
    val fechaCreacion: String,
    val estado: String = EstadoRegistro.PENDIENTE,
    val ubicacionesJson: String = ""
)

/** Un renglón tal como se envía a la API. */
data class RenglonEnvio(
    val codArticulo: String,
    val nombreUbicacion: String,
    val numPartida: String,
    val cantidad: Double
)

/** Payload completo de la recolección de un pedido. */
data class EnvioRecoleccion(
    val idPedido: Int,
    val codDeposito: String,
    val usuario: String,
    val renglones: List<RenglonEnvio>
)
