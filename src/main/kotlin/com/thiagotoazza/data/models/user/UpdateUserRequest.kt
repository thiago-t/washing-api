package com.thiagotoazza.data.models.user

data class UpdateUserRequest(
    val username: String?,
    val email: String?,
    val role: String?,
    val companyIds: List<String>?,
)