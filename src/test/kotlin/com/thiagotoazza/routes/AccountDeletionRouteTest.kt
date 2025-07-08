package com.thiagotoazza.routes

import com.thiagotoazza.data.models.accountdeletion.AccountDeletionRequest
import com.thiagotoazza.data.models.user.User
import com.thiagotoazza.data.source.user.UserDataSource
import com.thiagotoazza.security.hashing.HashingService
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
import org.mockito.kotlin.*
import kotlin.test.assertEquals

class AccountDeletionRouteTest {

    private val mockUserDataSource: UserDataSource = mock()
    private val mockHashingService: HashingService = mock()
    private val accountDeletionRoute = AccountDeletionRoute(
        mockUserDataSource,
        mockHashingService
    )

    @Test
    fun `test delete account with valid password`() = testApplication {
        val userId = ObjectId().toString()
        val user = User(
            username = "testuser",
            email = "test@example.com",
            password = "hashedPassword",
            role = "user",
            companyIds = listOf(),
            salt = "salt123"
        )

        whenever(mockUserDataSource.getUserById(userId)).thenReturn(user)
        whenever(mockHashingService.verifySaltedHash(any(), any())).thenReturn(true)
        whenever(mockUserDataSource.deleteUser(userId)).thenReturn(true)

        routing {
            accountDeletionRoute.run { deleteAccount() }
        }

        client.delete("/delete-account") {
            setBody(AccountDeletionRequest(password = "correctPassword"))
            header("Authorization", "Bearer valid-token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `test delete account with invalid password`() = testApplication {
        val userId = ObjectId().toString()
        val user = User(
            username = "testuser",
            email = "test@example.com",
            password = "hashedPassword",
            role = "user",
            companyIds = listOf(),
            salt = "salt123"
        )

        whenever(mockUserDataSource.getUserById(userId)).thenReturn(user)
        whenever(mockHashingService.verifySaltedHash(any(), any())).thenReturn(false)

        routing {
            accountDeletionRoute.run { deleteAccount() }
        }

        client.delete("/delete-account") {
            setBody(AccountDeletionRequest(password = "wrongPassword"))
            header("Authorization", "Bearer valid-token")
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `test delete account without password`() = testApplication {
        routing {
            accountDeletionRoute.run { deleteAccount() }
        }

        client.delete("/delete-account") {
            header("Authorization", "Bearer valid-token")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `test delete account with empty password`() = testApplication {
        routing {
            accountDeletionRoute.run { deleteAccount() }
        }

        client.delete("/delete-account") {
            setBody(AccountDeletionRequest(password = ""))
            header("Authorization", "Bearer valid-token")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }
} 