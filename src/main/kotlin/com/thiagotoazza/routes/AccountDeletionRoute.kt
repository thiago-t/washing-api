package com.thiagotoazza.routes

import com.thiagotoazza.data.models.accountdeletion.AccountDeletionRequest
import com.thiagotoazza.data.source.user.UserDataSource
import com.thiagotoazza.security.hashing.HashingService
import com.thiagotoazza.security.hashing.SaltedHash
import com.thiagotoazza.utils.ResponseError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class AccountDeletionRoute(
    private val userDataSource: UserDataSource,
    private val hashingService: HashingService
) {
    
    fun Route.deleteAccount() {
        authenticate {
            delete("/delete-account") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)
                
                if (userId == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ResponseError(HttpStatusCode.Unauthorized.value, "User not authenticated")
                    )
                    return@delete
                }

                val request = call.receiveNullable<AccountDeletionRequest>() ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Password is required")
                    )
                    return@delete
                }

                if (request.password.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Password cannot be empty")
                    )
                    return@delete
                }

                // Get the user to verify password
                val user = userDataSource.getUserById(userId) ?: run {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ResponseError(HttpStatusCode.NotFound.value, "User not found")
                    )
                    return@delete
                }

                // Verify the provided password
                val isValidPassword = hashingService.verifySaltedHash(
                    value = request.password,
                    saltedHash = SaltedHash(
                        hash = user.password,
                        salt = user.salt
                    )
                )

                if (!isValidPassword) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ResponseError(HttpStatusCode.Unauthorized.value, "Invalid password")
                    )
                    return@delete
                }

                // Delete the user account
                val userDeleted = userDataSource.deleteUser(userId)
                if (!userDeleted) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ResponseError(HttpStatusCode.InternalServerError.value, "Failed to delete user account")
                    )
                    return@delete
                }

                // Return success response - the client should handle logout
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("message" to "Account deleted successfully")
                )
            }
        }
    }
} 