package com.thiagotoazza.data.source.user

import com.thiagotoazza.data.models.user.User

interface UserDataSource {
    suspend fun getUserByEmail(email: String): User?
    suspend fun getUserById(id: String): User?
    suspend fun insertUser(user: User): Boolean
    suspend fun deleteUser(id: String): Boolean
}