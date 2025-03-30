package com.thiagotoazza.data.models.user

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val token: String
)