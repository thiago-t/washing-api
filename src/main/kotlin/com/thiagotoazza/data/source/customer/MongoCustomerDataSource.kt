package com.thiagotoazza.data.source.customer

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.customer.Customer
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId

class MongoCustomerDataSource(
    database: MongoDatabase
) : CustomerDataSource {
    private val customersCollection = database.getCollection<Customer>("customers")

    override suspend fun getCustomers(): List<Customer> {
        return customersCollection.find().toList()
    }

    override suspend fun getCustomerById(id: String?): Customer? {
        val query = Document("_id", ObjectId(id))
        return customersCollection.find<Customer>(query).firstOrNull()
    }

    override suspend fun insertCustomer(customer: Customer): Boolean {
        return customersCollection.insertOne(customer).wasAcknowledged()
    }

    override suspend fun deleteCustomer(id: String?): Boolean {
        val query = Document("_id", ObjectId(id))
        return customersCollection.deleteOne(query).wasAcknowledged()
    }
}