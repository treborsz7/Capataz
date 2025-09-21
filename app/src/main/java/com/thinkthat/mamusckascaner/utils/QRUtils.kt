package com.thinkthat.mamusckascaner.utils

data class QRData(
    val deposito: String = "",
    val pedido: String = ""
)

fun parseQRData(qrString: String): QRData {
    return try {
        // Formato esperado: |pikingsDePedido|Deposito|"codigoDeposito"|"idPedido"|
        val parts = qrString.split("|")
        
        if (parts.size >= 5) {
            val deposito = parts[3].replace("\"", "").trim()
            val pedido = parts[4].replace("\"", "").trim()
            
            QRData(deposito = deposito, pedido = pedido)
        } else {
            QRData()
        }
    } catch (e: Exception) {
        QRData()
    }
}
