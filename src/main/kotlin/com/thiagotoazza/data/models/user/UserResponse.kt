package com.thiagotoazza.data.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val role: String,
    val creationDate: Int,
    val companyIds: List<String>?
)
