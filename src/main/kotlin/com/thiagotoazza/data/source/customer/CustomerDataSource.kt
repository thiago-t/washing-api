package com.thiagotoazza.data.source.customer

import com.mongodb.kotlin.client.coroutine.ClientSession
import com.thiagotoazza.data.models.customer.Customer
import com.thiagotoazza.utils.ApiResult

interface CustomerDataSource {
    suspend fun getCustomers(): List<Customer>
    suspend fun getCustomersFromWasher(washerId: String): List<Customer>
    suspend fun getCustomerById(id: String?): Customer?
    suspend fun insertCustomer(customer: Customer): Boolean
    suspend fun insertCustomerV2(customer: Customer, session: ClientSession? = null): ApiResult
    suspend fun updateCustomer(customer: Customer): Boolean
    suspend fun deleteCustomer(id: String?): Boolean
}