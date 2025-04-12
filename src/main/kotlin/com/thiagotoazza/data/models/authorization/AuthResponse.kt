package com.thiagotoazza.data.models.authorization

import com.thiagotoazza.data.models.user.UserResponse
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val user: UserResponse,
    val token: String
)