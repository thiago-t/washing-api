package com.thiagotoazza.data.models.accountdeletion

import kotlinx.serialization.Serializable

@Serializable
data class AccountDeletionRequest(
    val password: String
) 