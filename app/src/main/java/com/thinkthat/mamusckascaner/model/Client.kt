package com.thinkthat.mamusckascaner.model

import kotlinx.serialization.Serializable

@Serializable
data class Client(
    val name: String,
    val email: String,
    val address: String
)
