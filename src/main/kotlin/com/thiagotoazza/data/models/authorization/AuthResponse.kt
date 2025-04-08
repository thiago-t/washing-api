package com.thiagotoazza.data.models.authorization

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val token: String
)