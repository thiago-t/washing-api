package com.thiagotoazza.data.models.user

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val username: String,
    val email: String,
    val role: String,
    val companyIds: List<String>? = null,
    val password: String
)