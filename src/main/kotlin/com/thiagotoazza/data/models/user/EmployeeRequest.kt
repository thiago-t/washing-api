package com.thiagotoazza.data.models.user

import kotlinx.serialization.Serializable

@Serializable
data class EmployeeRequest(
    val username: String,
    val email: String,
    val password: String
)
