package com.thiagotoazza.data.models.customer

data class CustomerResponse(
    val id: String,
    val fullName: String,
    val phoneNumber: String?,
    val washerId: String?
)
