package com.thiagotoazza.data.models.customer

data class CustomerResponse(
    val id: String,
    val fullName: String,
    val phoneNumber: String? = null,
    val isDeleted: Boolean? = null,
    val washerId: String? = null
)
