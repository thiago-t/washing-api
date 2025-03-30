package com.thiagotoazza.data.models.user

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class User(
    val username: String,
    val email: String,
    val password: String,
    val role: String,
    val companyIds: List<ObjectId>?,
    val salt: String,
    @BsonId val id: ObjectId = ObjectId()
)
