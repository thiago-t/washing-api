package com.thiagotoazza.data.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UpdateEmployeeRequest(
    val username: String? = null,
    val email: String? = null,
)
