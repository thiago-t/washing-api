package com.thiagotoazza.data.source.user

import com.mongodb.kotlin.client.coroutine.ClientSession
import com.thiagotoazza.data.models.user.User
import org.bson.conversions.Bson

interface UserDataSource {
    suspend fun getUserByEmail(email: String): User?
    suspend fun getUserById(id: String): User?
    suspend fun insertUser(user: User): Boolean
    suspend fun patchUser(userId: String, updates: List<Bson>): User?
    suspend fun patchUser(session: ClientSession, userId: String, updates: List<Bson>): User?
    suspend fun deleteUser(id: String): Boolean
}