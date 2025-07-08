package com.thiagotoazza.data.source.user

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.user.User
import com.thiagotoazza.utils.Constants
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.bson.types.ObjectId

class MongoUserDataSource(database: MongoDatabase) : UserDataSource {

    private val usersCollection = database.getCollection<User>(Constants.KEY_USERS_COLLECTION)

    override suspend fun getUserByEmail(email: String): User? {
        val query = Document(User::email.name, email)
        return usersCollection.find(query).firstOrNull()
    }

    override suspend fun getUserById(id: String): User? {
        val query = Document("_id", ObjectId(id))
        return usersCollection.find(query).firstOrNull()
    }

    override suspend fun insertUser(user: User): Boolean {
        return usersCollection.insertOne(user).wasAcknowledged()
    }

    override suspend fun deleteUser(id: String): Boolean {
        val query = Document("_id", ObjectId(id))
        return usersCollection.deleteOne(query).wasAcknowledged()
    }
}