package com.thiagotoazza.routes

import com.thiagotoazza.data.models.authorization.AuthRequest
import com.thiagotoazza.data.models.authorization.AuthResponse
import com.thiagotoazza.data.models.company.Address
import com.thiagotoazza.data.models.company.Company
import com.thiagotoazza.data.models.user.User
import com.thiagotoazza.data.source.company.CompanyDataSource
import com.thiagotoazza.data.source.user.UserDataSource
import com.thiagotoazza.security.hashing.HashingService
import com.thiagotoazza.security.hashing.SaltedHash
import com.thiagotoazza.security.token.TokenClaim
import com.thiagotoazza.security.token.TokenConfig
import com.thiagotoazza.security.token.TokenService
import com.thiagotoazza.utils.ResponseError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class AuthorizationRoute(
    private val userDataSource: UserDataSource,
    private val companyDataSource: CompanyDataSource,
    private val hashingService: HashingService,
    private val tokenService: TokenService,
    private val tokenConfig: TokenConfig
) {
    fun Route.signUp() {
        post("/signup") {
            val request = call.receiveNullable<AuthRequest>() ?: kotlin.run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseError(HttpStatusCode.BadRequest.value, "Invalid request body")
                )
                return@post
            }

            val areFieldsBlank = request.username.isBlank() || request.email.isBlank() || request.password.isBlank()
            val isPasswordTooShort = request.password.length < 8
            if (areFieldsBlank || isPasswordTooShort) {
                call.respond(
                    HttpStatusCode.Conflict,
                    ResponseError(HttpStatusCode.Conflict.value, "Invalid credentials")
                )
                return@post
            }

            val isExistingUser = userDataSource.getUserByEmail(request.email) != null
            if (isExistingUser) {
                call.respond(
                    HttpStatusCode.Conflict,
                    ResponseError(HttpStatusCode.Conflict.value, "User already exists")
                )
                return@post
            }

            // THE FOLLOWING CODE SNIPPET IS A TEMPORARY SOLUTION TO CREATE A COMPANY FOR NEW USERS
            val newCompany = buildCompany(userLastName = request.username.split(" ").lastOrNull())
            if (companyDataSource.insertCompany(company = newCompany)) {
                val saltedHash = hashingService.generateSaltedHash(request.password)
                val user = User(
                    username = request.username,
                    email = request.email,
                    password = saltedHash.hash,
                    role = request.role,
                    companyIds = listOf(newCompany.id),
                    salt = saltedHash.salt
                )
                val wasAcknowledged = userDataSource.insertUser(user)
                if (wasAcknowledged.not()) {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ResponseError(HttpStatusCode.Conflict.value, "Something went wrong")
                    )
                    return@post
                }

                call.respond(HttpStatusCode.Created)
            } else {
                call.respond(
                    HttpStatusCode.Conflict,
                    ResponseError(HttpStatusCode.Conflict.value, "An error occurred while creating user's company")
                )
                return@post
            }
        }
    }

    fun Route.signIn() {
        post("/signin") {
            val request = call.receiveNullable<AuthRequest>() ?: kotlin.run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseError(HttpStatusCode.BadRequest.value, "Invalid request body")
                )
                return@post
            }

            val user = userDataSource.getUserByEmail(request.email)
            if (user == null) {
                call.respond(
                    HttpStatusCode.Conflict,
                    ResponseError(HttpStatusCode.Conflict.value, "Incorrect username or password")
                )
                return@post
            }

            val isValidPassword = hashingService.verifySaltedHash(
                value = request.password,
                saltedHash = SaltedHash(
                    hash = user.password,
                    salt = user.salt
                )
            )

            if (isValidPassword.not()) {
                call.respond(
                    HttpStatusCode.Conflict,
                    ResponseError(HttpStatusCode.Conflict.value, "Incorrect username or password")
                )
                return@post
            }

            val token = tokenService.generateToken(
                config = tokenConfig,
                TokenClaim(
                    name = "userId",
                    value = user.id.toString()
                )
            )

            user.let {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = AuthResponse(
                        user = it.toUserResponse(),
                        token = token
                    )
                )
            }
        }
    }

    fun Route.authenticate() {
        authenticate {
            get("/authenticate") {
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    fun Route.getSecretInfo() {
        authenticate {
            get("/secret") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)
                call.respond(HttpStatusCode.OK, "Your secret info is $userId")
            }
        }
    }

    private fun buildCompany(userLastName: String?): Company {
        return Company(
            companyName = "$userLastName's Company",
            address = Address(),
        )
    }

}