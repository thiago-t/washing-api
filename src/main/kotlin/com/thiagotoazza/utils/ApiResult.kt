package com.thiagotoazza.utils

import org.bson.BsonObjectId

sealed class ApiResult {
    data class Success(
        val insertedId: BsonObjectId?,
    ) : ApiResult()

    data class Error(
        val message: String,
        val exception: Exception? = null
    ) : ApiResult()
}
