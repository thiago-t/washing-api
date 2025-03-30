package com.thiagotoazza.data.source.user

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.user.User
import com.thiagotoazza.utils.Constants
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document

class MongoUserDataSource(database: MongoDatabase) : UserDataSource {

    private val usersCollection = database.getCollection<User>(Constants.KEY_USERS_COLLECTION)

    override suspend fun getUserByUsername(username: String): User? {
        val query = Document(User::username.name, username)
        return usersCollection.find(query).firstOrNull()
    }

    override suspend fun insertUser(user: User): Boolean {
        return usersCollection.insertOne(user).wasAcknowledged()
    }
}