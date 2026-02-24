package com.thiagotoazza.utils

import org.bson.types.ObjectId

data class ApiResult(
    val wasAcknowledged: Boolean,
    val insertedId: ObjectId?
)
