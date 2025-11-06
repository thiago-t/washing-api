package com.thiagotoazza.routes

import com.thiagotoazza.data.models.company.Company
import com.thiagotoazza.data.source.company.CompanyDataSource
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.ResponseError
import com.thiagotoazza.utils.isValidObjectId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.types.ObjectId

class CompaniesRoute(
    private val companyDataSource: CompanyDataSource
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

                return@get call.respond(company)
            }

            post {
                val request = call.receiveNullable<Company>() ?: run {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid body!")
                    )
                }

                if (companyDataSource.insertCompany(company = request)) {
                    return@post call.respond(
                        HttpStatusCode.Created,
                        request
                    )
                } else {
                    return@post call.respond(HttpStatusCode.Conflict)
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
                        address = address,
                        companyName = companyName
                    )
                }

                if (companyDataSource.updateCompany(company = updatedCompany)) {
                    call.respond(HttpStatusCode.OK, updatedCompany)
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