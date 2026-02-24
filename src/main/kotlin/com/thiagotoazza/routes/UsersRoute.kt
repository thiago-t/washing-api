package com.thiagotoazza.routes

import com.mongodb.client.model.Updates
import com.thiagotoazza.data.models.accountdeletion.AccountDeletionRequest
import com.thiagotoazza.data.models.user.UpdateUserRequest
import com.thiagotoazza.data.models.user.User
import com.thiagotoazza.data.source.user.UserDataSource
import com.thiagotoazza.security.hashing.HashingService
import com.thiagotoazza.security.hashing.SaltedHash
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.ResponseError
import com.thiagotoazza.utils.isValidObjectId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.conversions.Bson
import org.bson.types.ObjectId

class UsersRoute(
    private val userDataSource: UserDataSource,
    private val hashingService: HashingService
) {

    fun Route.usersRoute() =
        route("/users") {
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

            patch("/{id}") {
                val userId = call.parameters[Constants.KEY_ID]

                if (userId.isValidObjectId().not()) {
                    return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid user ID")
                    )
                }

                val existingUser = userDataSource.getUserById(id = userId.orEmpty())
                    ?: return@patch call.respond(
                        HttpStatusCode.NotFound,
                        ResponseError(HttpStatusCode.NotFound.value, "User $userId not found")
                    )

                val toUpdateUser = call.receiveNullable<UpdateUserRequest>()
                    ?: return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid body request")
                    )

                val updates = updateFieldsBson(toUpdateUser)
                val updatedUser = userDataSource.patchUser(
                    userId = userId.orEmpty(),
                    updates = updates,
                )

                if (updatedUser != null) {
                    return@patch call.respond(
                        HttpStatusCode.OK,
                        updatedUser.toUserResponse()
                    )
                }
            }
        }
}

private fun updateFieldsBson(request: UpdateUserRequest): List<Bson> {
    val updates = mutableListOf<Bson>()

    request.username?.let { updates.add(Updates.set(User::username.name, it)) }
    request.email?.let { email -> updates.add(Updates.set(User::email.name, email)) }
    request.role?.let { role -> updates.add(Updates.set(User::role.name, role)) }
    request.companyIds
        ?.map { companyId -> ObjectId(companyId) }
        ?.let { companyIds -> updates.add(Updates.set(User::companyIds.name, companyIds)) }

    return updates
}
