package com.thiagotoazza.routes

import com.thiagotoazza.data.models.expenses.Expense
import com.thiagotoazza.data.models.expenses.ExpenseDeleteMode
import com.thiagotoazza.data.models.expenses.ExpenseRequest
import com.thiagotoazza.data.models.expenses.toExpenseResponse
import com.thiagotoazza.data.source.expense.ExpenseDataSource
import com.thiagotoazza.utils.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.BsonDateTime
import org.bson.types.ObjectId
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

class ExpensesRoute(
    private val expenseDataSource: ExpenseDataSource,
) {

    fun Route.expensesRoute() {
        route("/expenses") {
            get {
                val washerId = call.parameters[Constants.KEY_WASHER_ID].orEmpty()
                val dateQueryParam = call.request.queryParameters[Constants.KEY_DATE].orEmpty()

                if (!washerId.isValidObjectId()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                val expenses = if (dateQueryParam.isBlank()) {
                    expenseDataSource.materializeRecurringExpenses(washerId, YearMonth.now(ZoneOffset.UTC))
                    expenseDataSource.getExpenses(washerId)
                } else {
                    val dateFilter = try {
                        DateFilter.parse(dateQueryParam)
                    } catch (e: DateTimeParseException) {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ResponseError(HttpStatusCode.BadRequest.value, "Invalid expense date")
                        )
                    }

                    val targetYearMonth = when (dateFilter) {
                        is DateFilter.YearMonth -> YearMonth.of(dateFilter.year, dateFilter.month)
                        is DateFilter.FullDate -> {
                            val local = dateFilter.startDate.atZone(ZoneOffset.UTC)
                            YearMonth.of(local.year, local.monthValue)
                        }

                        is DateFilter.Year -> YearMonth.now(ZoneOffset.UTC)
                    }
                    expenseDataSource.materializeRecurringExpenses(washerId, targetYearMonth)
                    expenseDataSource.getExpensesByDate(washerId, dateFilter)
                }

                call.respond(HttpStatusCode.OK, expenses.map { it.toExpenseResponse() })
            }

            get("/{id}") {
                val expenseId = call.parameters[Constants.KEY_ID]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (!expenseId.isValidObjectId()) {
                    return@get call.respond(HttpStatusCode.BadRequest)
                }

                val expense = expenseDataSource.getExpenseById(expenseId)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        ResponseError(HttpStatusCode.NotFound.value, "Expense not found")
                    )

                call.respond(HttpStatusCode.OK, expense.toExpenseResponse())
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
                val recurringGroupId = if (request.isRecurringMonthly) {
                    if (request.recurringGroupId.isNullOrBlank()) ObjectId() else ObjectId(request.recurringGroupId)
                } else null

                val recurrenceDayOfMonth = if (request.isRecurringMonthly) {
                    Instant.ofEpochMilli(request.dueDate).atZone(ZoneOffset.UTC).dayOfMonth
                } else null

                val expense = Expense(
                    washerId = ObjectId(washerId),
                    amount = request.amount,
                    category = request.category,
                    isPaidToday = request.isPaidToday,
                    dueDate = BsonDateTime(request.dueDate),
                    isRecurringMonthly = request.isRecurringMonthly,
                    recurringGroupId = recurringGroupId,
                    recurrenceDayOfMonth = recurrenceDayOfMonth,
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

            put("/{id}") {
                val washerId = call.parameters[Constants.KEY_WASHER_ID].orEmpty()
                val expenseId = call.parameters[Constants.KEY_ID]
                    ?: return@put call.respond(HttpStatusCode.BadRequest)

                if (!washerId.isValidObjectId() || !expenseId.isValidObjectId()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer or expense ID")
                    )
                }

                val request = call.receive<ExpenseRequest>()
                val existing = expenseDataSource.getExpenseById(expenseId)

                val recurringGroupId = if (request.isRecurringMonthly) {
                    when {
                        !request.recurringGroupId.isNullOrBlank() -> ObjectId(request.recurringGroupId)
                        existing?.recurringGroupId != null -> existing.recurringGroupId
                        else -> ObjectId()
                    }
                } else null

                val recurrenceDayOfMonth = if (request.isRecurringMonthly) {
                    Instant.ofEpochMilli(request.dueDate).atZone(ZoneOffset.UTC).dayOfMonth
                } else null

                val updatedExpense = Expense(
                    id = ObjectId(expenseId),
                    washerId = ObjectId(washerId),
                    amount = request.amount,
                    category = request.category,
                    isPaidToday = request.isPaidToday,
                    dueDate = BsonDateTime(request.dueDate),
                    isRecurringMonthly = request.isRecurringMonthly,
                    recurringGroupId = recurringGroupId,
                    recurrenceDayOfMonth = recurrenceDayOfMonth,
                    recurrenceEndDate = existing?.recurrenceEndDate,
                    description = request.description
                )

                if (expenseDataSource.updateExpense(updatedExpense)) {
                    call.respond(HttpStatusCode.OK, updatedExpense.toExpenseResponse())
                } else {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ResponseError(HttpStatusCode.Conflict.value, "Failed to update expense")
                    )
                }
            }

            delete("/{id}") {
                val washerId = call.parameters[Constants.KEY_WASHER_ID].orEmpty()
                val expenseId = call.parameters[Constants.KEY_ID]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)

                if (!washerId.isValidObjectId() || !expenseId.isValidObjectId()) {
                    return@delete call.respond(HttpStatusCode.BadRequest)
                }

                val modeParam = call.request.queryParameters["mode"]
                val mode = ExpenseDeleteMode.fromString(modeParam)

                if (expenseDataSource.deleteExpenseWithMode(washerId, expenseId, mode)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.Conflict)
                }
            }
        }
    }
}
