package com.thiagotoazza.data

import com.mongodb.kotlin.client.coroutine.MongoClient

object WashingDatabase {
    private val mongoPassword = System.getenv("MONGO_PW")
    private val mongoClient = MongoClient.create(
        connectionString = "mongodb+srv://toazzat:$mongoPassword@washing.mmghon8.mongodb.net/"
    )
    val database = mongoClient.getDatabase("washing-db")
}
