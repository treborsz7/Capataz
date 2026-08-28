package com.thinkthat.mamusckascaner.domain.model

/** Depósito y pedido codificados en el QR de recolección. */
data class DatosQr(
    val deposito: String = "",
    val pedido: String = ""
) {
    val esValido: Boolean
        get() = deposito.isNotEmpty() && pedido.isNotEmpty()

    val idPedido: Int?
        get() = pedido.toIntOrNull()
}

/**
 * Formato esperado: |pikingsDePedido|Deposito|"codigoDeposito"|"idPedido"|
 * Cualquier otra cosa devuelve [DatosQr] vacío, que [DatosQr.esValido] rechaza.
 */
fun parsearQr(contenido: String): DatosQr {
    val partes = contenido.split("|")
    if (partes.size < 5) return DatosQr()

    return DatosQr(
        deposito = partes[3].replace("\"", "").trim(),
        pedido = partes[4].replace("\"", "").trim()
    )
}

/** Reconstruye el contenido del QR, para retomar un pedido ya guardado. */
fun DatosQr.aContenidoQr(): String = "|pikingsDePedido|Deposito|\"$deposito\"|\"$pedido\"|"
