package com.thinkthat.mamusckascaner.model

import java.io.Serializable

data class OrdenTrabajo(
    val nroorden: Int,
    val listadoitems: List<ItemOrden>,
    val fechahora: String,
    val estado: String
) : Serializable

data class ItemOrden(
    val nropartida: String,
    val ubicacion: String,
    val cantidad: Int
) : Serializable
