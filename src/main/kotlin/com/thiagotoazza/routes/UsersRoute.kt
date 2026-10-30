package com.thiagotoazza.routes

import com.mongodb.client.model.Updates
import com.thiagotoazza.data.models.accountdeletion.AccountDeletionRequest
import com.thiagotoazza.data.models.user.EmployeeErrorCode
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
                            ResponseError(
                                HttpStatusCode.Unauthorized.value,
                                EmployeeErrorCode.USER_NOT_AUTHENTICATED.name
                            )
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

                get("/employees") {
                    val principal = call.principal<JWTPrincipal>()
                    val managerId = principal?.getClaim("userId", String::class)

                    if (managerId == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ResponseError(
                                HttpStatusCode.Unauthorized.value,
                                EmployeeErrorCode.USER_NOT_AUTHENTICATED.name
                            )
                        )
                        return@get
                    }

                    val manager = userDataSource.getUserById(managerId)
                    if (manager == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ResponseError(HttpStatusCode.NotFound.value, EmployeeErrorCode.MANAGER_NOT_FOUND.name)
                        )
                        return@get
                    }

                    val companyId = manager.companyIds?.firstOrNull()
                    if (companyId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ResponseError(HttpStatusCode.BadRequest.value, EmployeeErrorCode.COMPANY_NOT_SETUP.name)
                        )
                        return@get
                    }

                    val employees = userDataSource.getUsersByCompanyId(companyId.toString())
                    // Filter out the manager from the list if we only want employees, or return all team members
                    // For now, return all users in the company except the requester (if they just want to see employees)
                    // Let's return all users in the company for "Team Management"
                    val team = employees.map { it.toUserResponse() }
                    call.respond(HttpStatusCode.OK, team)
                }

                get("/employee/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val managerId = principal?.getClaim("userId", String::class)

                    if (managerId == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ResponseError(
                                HttpStatusCode.Unauthorized.value,
                                EmployeeErrorCode.USER_NOT_AUTHENTICATED.name
                            )
                        )
                        return@get
                    }

                    val manager = userDataSource.getUserById(managerId)
                    val companyId = manager?.companyIds?.firstOrNull()?.toString()

                    val employeeId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, EmployeeErrorCode.MISSING_EMPLOYEE_ID.name)
                    )

                    val employee = userDataSource.getUserById(employeeId)
                    if (employee == null || employee.companyIds?.firstOrNull()?.toString() != companyId) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ResponseError(HttpStatusCode.NotFound.value, EmployeeErrorCode.EMPLOYEE_NOT_FOUND.name)
                        )
                        return@get
                    }

                    call.respond(HttpStatusCode.OK, employee.toUserResponse())
                }

                delete("/employee/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val managerId = principal?.getClaim("userId", String::class)

                    if (managerId == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ResponseError(
                                HttpStatusCode.Unauthorized.value,
                                EmployeeErrorCode.USER_NOT_AUTHENTICATED.name
                            )
                        )
                        return@delete
                    }

                    val manager = userDataSource.getUserById(managerId)
                    val companyId = manager?.companyIds?.firstOrNull()?.toString()

                    val employeeId = call.parameters["id"] ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, EmployeeErrorCode.MISSING_EMPLOYEE_ID.name)
                    )

                    val employee = userDataSource.getUserById(employeeId)
                    if (employee == null || employee.companyIds?.firstOrNull()?.toString() != companyId) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ResponseError(HttpStatusCode.Forbidden.value, EmployeeErrorCode.UNAUTHORIZED_DELETE.name)
                        )
                        return@delete
                    }
                    if (employee.id.toString() == managerId) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ResponseError(HttpStatusCode.Forbidden.value, EmployeeErrorCode.CANNOT_DELETE_SELF.name)
                        )
                        return@delete
                    }

                    val deleted = userDataSource.deleteUser(employeeId)
                    if (deleted) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Employee deleted successfully"))
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ResponseError(
                                HttpStatusCode.InternalServerError.value,
                                EmployeeErrorCode.FAILED_TO_DELETE.name
                            )
                        )
                    }
                }

                put("/employee/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val managerId = principal?.getClaim("userId", String::class)

                    if (managerId == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ResponseError(
                                HttpStatusCode.Unauthorized.value,
                                EmployeeErrorCode.USER_NOT_AUTHENTICATED.name
                            )
                        )
                        return@put
                    }

                    val manager = userDataSource.getUserById(managerId)
                    val companyId = manager?.companyIds?.firstOrNull()?.toString()

                    val employeeId = call.parameters["id"] ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, EmployeeErrorCode.MISSING_EMPLOYEE_ID.name)
                    )

                    val request = call.receiveNullable<com.thiagotoazza.data.models.user.UpdateEmployeeRequest>()
                        ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ResponseError(HttpStatusCode.BadRequest.value, EmployeeErrorCode.INVALID_REQUEST_BODY.name)
                        )

                    val employee = userDataSource.getUserById(employeeId)
                    if (employee == null || employee.companyIds?.firstOrNull()?.toString() != companyId) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ResponseError(HttpStatusCode.Forbidden.value, EmployeeErrorCode.UNAUTHORIZED_UPDATE.name)
                        )
                        return@put
                    }

                    if (request.email != null && request.email != employee.email) {
                        val existingUser = userDataSource.getUserByEmail(request.email)
                        if (existingUser != null) {
                            call.respond(
                                io.ktor.http.HttpStatusCode.Conflict,
                                com.thiagotoazza.utils.ResponseError(
                                    io.ktor.http.HttpStatusCode.Conflict.value,
                                    EmployeeErrorCode.EMAIL_ALREADY_EXISTS.name
                                )
                            )
                            return@put
                        }
                    }

                    val updates = mutableListOf<org.bson.conversions.Bson>()
                    if (request.username != null) updates.add(
                        com.mongodb.client.model.Updates.set(
                            com.thiagotoazza.data.models.user.User::username.name,
                            request.username
                        )
                    )
                    if (request.email != null) updates.add(
                        com.mongodb.client.model.Updates.set(
                            com.thiagotoazza.data.models.user.User::email.name,
                            request.email
                        )
                    )

                    if (updates.isEmpty()) {
                        call.respond(HttpStatusCode.OK, employee.toUserResponse())
                        return@put
                    }

                    val updatedUser = userDataSource.patchUser(employeeId, updates)
                    if (updatedUser != null) {
                        call.respond(HttpStatusCode.OK, updatedUser.toUserResponse())
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ResponseError(
                                HttpStatusCode.InternalServerError.value,
                                EmployeeErrorCode.FAILED_TO_UPDATE.name
                            )
                        )
                    }
                }

                post("/employee") {
                    val principal = call.principal<JWTPrincipal>()
                    val managerId = principal?.getClaim("userId", String::class)

                    if (managerId == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ResponseError(
                                HttpStatusCode.Unauthorized.value,
                                EmployeeErrorCode.USER_NOT_AUTHENTICATED.name
                            )
                        )
                        return@post
                    }

                    val manager = userDataSource.getUserById(managerId)
                    if (manager == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ResponseError(HttpStatusCode.NotFound.value, EmployeeErrorCode.MANAGER_NOT_FOUND.name)
                        )
                        return@post
                    }

                    val companyId = manager.companyIds?.firstOrNull()
                    if (companyId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ResponseError(HttpStatusCode.BadRequest.value, EmployeeErrorCode.COMPANY_NOT_SETUP.name)
                        )
                        return@post
                    }

                    // Enforce plan limits (Mocking Free plan = 1 User limit)
                    val userCount = userDataSource.countUsersByCompanyId(companyId.toString())
                    val maxUsersAllowed = 2 // Simulating the Free Plan limit where only the Manager is allowed
                    if (userCount >= maxUsersAllowed) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ResponseError(HttpStatusCode.Forbidden.value, EmployeeErrorCode.LIMIT_REACHED.name)
                        )
                        return@post
                    }

                    val request = call.receiveNullable<com.thiagotoazza.data.models.user.EmployeeRequest>() ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ResponseError(HttpStatusCode.BadRequest.value, EmployeeErrorCode.INVALID_REQUEST_BODY.name)
                        )
                        return@post
                    }

                    val existingUser = userDataSource.getUserByEmail(request.email)
                    if (existingUser != null) {
                        call.respond(
                            HttpStatusCode.Conflict,
                            ResponseError(HttpStatusCode.Conflict.value, EmployeeErrorCode.EMAIL_ALREADY_EXISTS.name)
                        )
                        return@post
                    }

                    val saltedHash = hashingService.generateSaltedHash(request.password)
                    val employeeUser = User(
                        username = request.username,
                        email = request.email,
                        password = saltedHash.hash,
                        role = com.thiagotoazza.utils.Constants.ROLE_EMPLOYEE,
                        companyIds = listOf(companyId),
                        salt = saltedHash.salt
                    )

                    val inserted = userDataSource.insertUser(employeeUser)
                    if (inserted) {
                        call.respond(HttpStatusCode.Created, employeeUser.toUserResponse())
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ResponseError(
                                HttpStatusCode.InternalServerError.value,
                                EmployeeErrorCode.FAILED_TO_CREATE.name
                            )
                        )
                    }
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
