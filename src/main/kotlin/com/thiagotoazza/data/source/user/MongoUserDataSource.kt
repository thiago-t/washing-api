package com.thiagotoazza.data.source.user

import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.user.User
import com.thiagotoazza.utils.Constants
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.bson.conversions.Bson
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

    override suspend fun patchUser(userId: String, updates: List<Bson>): User? {
        if (updates.isEmpty()) {
            return getUserById(userId)
        }
        val combinedUpdates = Updates.combine(updates)
        val query = Document("_id", ObjectId(userId))
        val options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        return usersCollection.findOneAndUpdate(query, combinedUpdates, options)
    }

    override suspend fun patchUser(session: ClientSession, userId: String, updates: List<Bson>): User? {
        if (updates.isEmpty()) {
            return getUserById(userId)
        }
        val combinedUpdates = Updates.combine(updates)
        val query = Document("_id", ObjectId(userId))
        val options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        return usersCollection.findOneAndUpdate(session, query, combinedUpdates, options)
    }

    override suspend fun deleteUser(id: String): Boolean {
        val query = Document("_id", ObjectId(id))
        return usersCollection.deleteOne(query).wasAcknowledged()
    }

}
