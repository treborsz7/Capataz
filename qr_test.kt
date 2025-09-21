// Test file to verify QR parsing logic

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

fun main() {
    // Test cases
    val testQR1 = "|pikingsDePedido|Deposito|\"DEP001\"|\"PED12345\"|"
    val testQR2 = "|pikingsDePedido|Deposito|\"ALMACEN-A\"|\"ORDER-999\"|"
    
    println("Test 1:")
    val result1 = parseQRData(testQR1)
    println("Deposito: ${result1.deposito}")
    println("Pedido: ${result1.pedido}")
    println()
    
    println("Test 2:")
    val result2 = parseQRData(testQR2)
    println("Deposito: ${result2.deposito}")
    println("Pedido: ${result2.pedido}")
    println()
    
    // Test invalid format
    val testQR3 = "invalid_format"
    println("Test 3 (Invalid):")
    val result3 = parseQRData(testQR3)
    println("Deposito: '${result3.deposito}' (should be empty)")
    println("Pedido: '${result3.pedido}' (should be empty)")
}
