package com.thiagotoazza.data.models.company

data class CompanyResponse(
    val id: String,
    val companyName: String,
    val phoneNumber: String,
    val address: Address,
)
