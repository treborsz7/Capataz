package com.codegalaxy.barcodescanner.model


import kotlinx.serialization.Serializable

@Serializable
data class BarModel(
    val invoiceNumber: String,
    val client: Client,
    val purchase: List<PurchaseItem>,
    val totalAmount: Double
)
