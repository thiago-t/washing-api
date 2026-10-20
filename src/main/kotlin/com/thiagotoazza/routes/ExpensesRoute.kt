package com.thiagotoazza.routes

import com.thiagotoazza.data.models.expenses.Expense
import com.thiagotoazza.data.models.expenses.ExpenseRequest
import com.thiagotoazza.data.models.expenses.toExpenseResponse
import com.thiagotoazza.data.source.expense.ExpenseDataSource
import com.thiagotoazza.utils.ApiResult
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.ResponseError
import com.thiagotoazza.utils.isValidObjectId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.bson.BsonDateTime
import org.bson.types.ObjectId

class ExpensesRoute(
    private val expenseDataSource: ExpenseDataSource,
) {

    fun Route.expensesRoute() {
        route("/expenses") {
            get {
                val washerId = call.parameters[Constants.KEY_WASHER_ID].orEmpty()
                if (!washerId.isValidObjectId()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                val expenses = expenseDataSource.getExpenses(washerId).map { it.toExpenseResponse() }
                call.respond(HttpStatusCode.OK, expenses)
            }

            post {
                val washerId = call.parameters[Constants.KEY_WASHER_ID].orEmpty()
                if (!washerId.isValidObjectId()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                val request = call.receive<ExpenseRequest>()
                val expense = Expense(
                    washerId = ObjectId(washerId),
                    amount = request.amount,
                    category = request.category,
                    isPaidToday = request.isPaidToday,
                    dueDate = BsonDateTime(request.dueDate),
                    isRecurringMonthly = request.isRecurringMonthly,
                    description = request.description
                )

                when (val result = expenseDataSource.insertExpense(expense)) {
                    is ApiResult.Success -> {
                        call.respond(HttpStatusCode.Created, expense.toExpenseResponse())
                    }

                    is ApiResult.Error -> {
                        call.respond(
                            status = HttpStatusCode.Conflict,
                            message = ResponseError(
                                code = HttpStatusCode.Conflict.value,
                                message = result.message
                            )
                        )
                    }
                }
            }

            delete("/{id}") {
                val expenseId = call.parameters[Constants.KEY_ID]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)
                if (!expenseId.isValidObjectId()) {
                    return@delete call.respond(HttpStatusCode.BadRequest)
                }

                if (expenseDataSource.deleteExpense(expenseId)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.Conflict)
                }
            }
        }
    }
}
