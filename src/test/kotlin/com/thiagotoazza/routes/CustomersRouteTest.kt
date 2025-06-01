package com.thiagotoazza.routes

import com.thiagotoazza.data.models.customer.Customer
import com.thiagotoazza.data.models.customer.CustomerRequest
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
import com.thiagotoazza.security.token.TokenConfig
import com.thiagotoazza.utils.Constants
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CustomersRouteTest {

    private val mockCustomerDataSource: MongoCustomerDataSource = mock()
    private val customersRoute = CustomersRoute(mockCustomerDataSource)
    private val testTokenConfig = TokenConfig(
        issuer = "http://0.0.0.0:8282",
        audience = "users",
        expiresIn = 365L * 1000L * 60L * 60L * 24L,
        secret = "test-secret"
    )

    @Test
    fun `test get all customers`() = testApplication {
        val customers = listOf(
            Customer(
                fullName = "John Doe",
                phoneNumber = "1234567890",
                washerId = ObjectId(),
                isDeleted = false
            ),
            Customer(
                fullName = "Jane Doe",
                phoneNumber = "0987654321",
                washerId = ObjectId(),
                isDeleted = false
            )
        )

        whenever(mockCustomerDataSource.getCustomers()).thenReturn(customers)

        routing {
            customersRoute.run { customersRoute() }
        }

        client.get("/customers").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertNotNull(body())
        }
    }

    @Test
    fun `test get customers by washer id`() = testApplication {
        val washerId = ObjectId().toString()
        val customers = listOf(
            Customer(
                fullName = "John Doe",
                phoneNumber = "1234567890",
                washerId = ObjectId(washerId),
                isDeleted = false
            )
        )

        whenever(mockCustomerDataSource.getCustomersFromWasher(washerId)).thenReturn(customers)

        routing {
            customersRoute.run { customersRoute() }
        }

        client.get("/customers?${Constants.KEY_WASHER_ID}=$washerId").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertNotNull(body())
        }
    }

    @Test
    fun `test get customer by id`() = testApplication {
        val customerId = ObjectId().toString()
        val customer = Customer(
            fullName = "John Doe",
            phoneNumber = "1234567890",
            washerId = ObjectId(),
            isDeleted = false
        )

        whenever(mockCustomerDataSource.getCustomerById(customerId)).thenReturn(customer)

        routing {
            customersRoute.run { customersRoute() }
        }

        client.get("/customers/$customerId").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertNotNull(body())
        }
    }

    @Test
    fun `test create customer`() = testApplication {
        val washerId = ObjectId().toString()
        val request = CustomerRequest(
            fullName = "John Doe",
            phoneNumber = "1234567890"
        )

        whenever(mockCustomerDataSource.insertCustomer(any())).thenReturn(true)

        routing {
            customersRoute.run { customersRoute() }
        }

        client.post("/customers?${Constants.KEY_WASHER_ID}=$washerId") {
            setBody(request)
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertNotNull(body())
        }
    }

    @Test
    fun `test update customer`() = testApplication {
        val customerId = ObjectId().toString()
        val washerId = ObjectId().toString()
        val request = CustomerRequest(
            fullName = "John Doe Updated",
            phoneNumber = "1234567890"
        )
        val existingCustomer = Customer(
            fullName = "John Doe",
            phoneNumber = "1234567890",
            washerId = ObjectId(washerId),
            isDeleted = false
        )

        whenever(mockCustomerDataSource.getCustomerById(customerId)).thenReturn(existingCustomer)
        whenever(mockCustomerDataSource.updateCustomer(any())).thenReturn(true)

        routing {
            customersRoute.run { customersRoute() }
        }

        client.put("/customers/$customerId?${Constants.KEY_WASHER_ID}=$washerId") {
            setBody(request)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertNotNull(body())
        }
    }

    @Test
    fun `test soft delete customer`() = testApplication {
        val customerId = ObjectId().toString()
        val washerId = ObjectId().toString()
        val customer = Customer(
            fullName = "John Doe",
            phoneNumber = "1234567890",
            washerId = ObjectId(washerId),
            isDeleted = false
        )

        whenever(mockCustomerDataSource.getCustomerById(customerId)).thenReturn(customer)
        whenever(mockCustomerDataSource.updateCustomer(any())).thenReturn(true)

        routing {
            customersRoute.run { customersRoute() }
        }

        client.delete("/customers/$customerId?${Constants.KEY_WASHER_ID}=$washerId").apply {
            assertEquals(HttpStatusCode.NoContent, status)
        }
    }

    @Test
    fun `test hard delete customer`() = testApplication {
        val customerId = ObjectId().toString()
        val washerId = ObjectId().toString()
        val customer = Customer(
            fullName = "John Doe",
            phoneNumber = "1234567890",
            washerId = ObjectId(washerId),
            isDeleted = false
        )

        whenever(mockCustomerDataSource.getCustomerById(customerId)).thenReturn(customer)
        whenever(mockCustomerDataSource.deleteCustomer(customerId)).thenReturn(true)

        routing {
            customersRoute.run { customersRoute() }
        }

        client.delete("/customers/$customerId?${Constants.KEY_WASHER_ID}=$washerId&force=true").apply {
            assertEquals(HttpStatusCode.NoContent, status)
        }
    }

    @Test
    fun `test get customer with invalid id`() = testApplication {
        routing {
            customersRoute.run { customersRoute() }
        }

        client.get("/customers/invalid-id").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `test create customer with invalid washer id`() = testApplication {
        val request = CustomerRequest(
            fullName = "John Doe",
            phoneNumber = "1234567890"
        )

        routing {
            customersRoute.run { customersRoute() }
        }

        client.post("/customers?${Constants.KEY_WASHER_ID}=invalid-id") {
            setBody(request)
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }
} 