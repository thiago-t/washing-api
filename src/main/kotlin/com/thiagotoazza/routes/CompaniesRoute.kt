package com.thiagotoazza.routes

import com.mongodb.client.model.Updates
import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.models.company.Company
import com.thiagotoazza.data.models.onboarding.OnboardingResponse
import com.thiagotoazza.data.models.user.User
import com.thiagotoazza.data.source.company.CompanyDataSource
import com.thiagotoazza.data.source.user.UserDataSource
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
import org.bson.types.ObjectId

class CompaniesRoute(
    private val companyDataSource: CompanyDataSource,
    private val userDataSource: UserDataSource,
) {

    fun Route.companiesRoute() {

        route("/companies") {
            get("/{id}") {
                val companyId = call.parameters[Constants.KEY_ID]

                if (companyId.isValidObjectId().not()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid company id $companyId")
                    )
                }

                val company = companyDataSource.getCompanyById(companyId) ?: run {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        ResponseError(HttpStatusCode.NotFound.value, "Company $companyId not found!")
                    )
                }

                return@get call.respond(company.toCompanyResponse())
            }

            post {
                val request = call.receiveNullable<Company>() ?: run {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid body!")
                    )
                }

                val insertedCompanyId = companyDataSource.insertCompany(company = request)
                val isValidCompanyId = insertedCompanyId?.isNull?.not()
                if (isValidCompanyId == true) {
                    val companyResponse = request
                        .copy(id = insertedCompanyId.asObjectId().value)
                        .toCompanyResponse()
                    return@post call.respond(
                        HttpStatusCode.Created,
                        companyResponse
                    )
                } else {
                    return@post call.respond(HttpStatusCode.Conflict)
                }
            }

            authenticate {
                post("/onboarding") {
                    val clientSession = WashingDatabase.clientSession()
                    clientSession.use { session ->
                        session.startTransaction()
                        try {
                            val principal = call.principal<JWTPrincipal>()

                            val userId = principal?.getClaim("userId", String::class)
                                ?: throw IllegalStateException("User not authenticated")

                            val doesUserExists = userDataSource.getUserById(id = userId)
                                ?: throw IllegalStateException("UserId $userId not found!")

                            val request = call.receiveNullable<Company>()
                                ?: throw IllegalStateException("Invalid request body!")

                            val insertedCompanyId = companyDataSource
                                .insertCompany(session = session, company = request)
                                ?.asObjectId()
                                ?.value
                                ?: throw IllegalStateException("An error occurred while creating the company")

                            val bsonUpdate = Updates.addToSet(User::companyIds.name, insertedCompanyId)

                            val updatedUser = userDataSource.patchUser(
                                session = session,
                                userId = userId,
                                updates = listOf(bsonUpdate)
                            ) ?: throw IllegalStateException("An error occurred while updating user data")

                            session.commitTransaction()
                            return@post call.respond(
                                HttpStatusCode.OK,
                                OnboardingResponse(
                                    user = updatedUser.toUserResponse(),
                                    company = request.copy(id = insertedCompanyId).toCompanyResponse()
                                )
                            )
                        } catch (exception: Exception) {
                            session.abortTransaction()
                            return@post call.respond(
                                HttpStatusCode.Conflict,
                                ResponseError(
                                    HttpStatusCode.Conflict.value,
                                    exception.message.orEmpty()
                                )
                            )
                        }
                    }
                }
            }

            put("/{id}") {
                val companyId = call.parameters[Constants.KEY_ID]

                if (companyId.isValidObjectId().not()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid company id $companyId")
                    )
                }

                val request = call.receiveNullable<Company>() ?: run {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Could not parse request body.")
                    )
                }

                val updatedCompany = with(request) {
                    Company(
                        id = ObjectId(companyId),
                        documentNumber = documentNumber,
                        companyName = companyName,
                        phoneNumber = phoneNumber,
                        address = address,
                    )
                }

                if (companyDataSource.updateCompany(company = updatedCompany)) {
                    call.respond(HttpStatusCode.OK, updatedCompany.toCompanyResponse())
                } else {
                    call.respond(HttpStatusCode.Conflict)
                }
            }

            delete("/{id}") {
                val companyId = call.parameters[Constants.KEY_ID]

                if (companyId.isValidObjectId().not()) {
                    return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid companyId id $companyId")
                    )
                }

                if (companyDataSource.deleteCompany(companyId)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.Conflict)
                }
            }
        }
    }

}