package com.thiagotoazza.data.source.customer

import com.thiagotoazza.data.models.customer.Customer

interface CustomerDataSource {
    suspend fun getCustomers(): List<Customer>
    suspend fun getCustomerById(id: String?): Customer?
    suspend fun insertCustomer(customer: Customer): Boolean
    suspend fun deleteCustomer(id: String?): Boolean
}