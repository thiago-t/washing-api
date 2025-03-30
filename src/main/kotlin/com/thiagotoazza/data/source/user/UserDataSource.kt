package com.thiagotoazza.data.source.user

import com.thiagotoazza.data.models.user.User

interface UserDataSource {
    suspend fun getUserByUsername(username: String): User?
    suspend fun insertUser(user: User): Boolean
}