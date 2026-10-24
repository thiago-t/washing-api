package com.thiagotoazza.data.source.expense

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.expenses.Expense
import com.thiagotoazza.data.models.expenses.ExpenseDeleteMode
import com.thiagotoazza.utils.ApiResult
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.DateFilter
import com.thiagotoazza.utils.toApiResult
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.BsonDateTime
import org.bson.Document
import org.bson.types.ObjectId
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

class MongoExpenseDataSource(database: MongoDatabase) : ExpenseDataSource {

    private val expensesCollection = database.getCollection<Expense>(Constants.KEY_EXPENSES_COLLECTION)

    override suspend fun getExpenses(washerId: String): List<Expense> {
        val filter = Filters.and(
            Filters.eq(Constants.KEY_WASHER_ID, ObjectId(washerId)),
            Filters.ne(Expense::isDeleted.name, true)
        )
        return expensesCollection.find(filter)
            .sort(Document(Expense::dueDate.name, -1))
            .toList()
    }

    override suspend fun getExpensesByDate(
        washerId: String,
        dateFilter: DateFilter
    ): List<Expense> {
        val filter = Filters.and(
            Filters.eq(Constants.KEY_WASHER_ID, ObjectId(washerId)),
            Filters.gte(Expense::dueDate.name, dateFilter.startDate),
            Filters.lte(Expense::dueDate.name, dateFilter.endDate),
            Filters.ne(Expense::isDeleted.name, true)
        )
        return expensesCollection.find(filter)
            .sort(Document(Expense::dueDate.name, -1))
            .toList()
    }

    override suspend fun getExpenseById(id: String): Expense? {
        val filter = Filters.and(
            Filters.eq("_id", ObjectId(id)),
            Filters.ne(Expense::isDeleted.name, true)
        )
        return expensesCollection.find(filter).firstOrNull()
    }

    override suspend fun insertExpense(expense: Expense): ApiResult {
        return expensesCollection.insertOne(expense).toApiResult()
    }

    override suspend fun updateExpense(expense: Expense): Boolean {
        val query = Document("_id", expense.id)
        return expensesCollection.replaceOne(query, expense).wasAcknowledged()
    }

    override suspend fun deleteExpense(id: String): Boolean {
        val query = Document("_id", ObjectId(id))
        val update = Document("\$set", Document(Expense::isDeleted.name, true))
        return expensesCollection.updateOne(query, update).wasAcknowledged()
    }

    override suspend fun deleteExpenseWithMode(
        washerId: String,
        id: String,
        mode: ExpenseDeleteMode
    ): Boolean {
        val expense = getExpenseById(id) ?: return false
        val washerObjectId = ObjectId(washerId)
        val expenseObjectId = ObjectId(id)

        if (expense.washerId != washerObjectId) {
            return false
        }

        val recurringGroupId = expense.recurringGroupId

        return when (mode) {
            ExpenseDeleteMode.THIS_ONLY -> {
                val query = Document("_id", expenseObjectId)
                val update = Document("\$set", Document(Expense::isDeleted.name, true))
                expensesCollection.updateOne(query, update).wasAcknowledged()
            }

            ExpenseDeleteMode.THIS_AND_FUTURE -> {
                val localDate = Instant.ofEpochMilli(expense.dueDate.value).atZone(ZoneOffset.UTC).toLocalDate()
                val monthStart =
                    YearMonth.of(localDate.year, localDate.monthValue).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                val cutoffBson = BsonDateTime(monthStart.toEpochMilli())

                val deleteFilter = Filters.and(
                    Filters.eq(Constants.KEY_WASHER_ID, washerObjectId),
                    Filters.eq(Expense::recurringGroupId.name, recurringGroupId ?: expenseObjectId),
                    Filters.gte(Expense::dueDate.name, cutoffBson)
                )
                val deleteUpdate = Document("\$set", Document(Expense::isDeleted.name, true))
                expensesCollection.updateMany(deleteFilter, deleteUpdate)

                if (recurringGroupId != null) {
                    val previousEndDate = BsonDateTime(monthStart.toEpochMilli() - 1)
                    val updateFilter = Filters.and(
                        Filters.eq(Constants.KEY_WASHER_ID, washerObjectId),
                        Filters.eq(Expense::recurringGroupId.name, recurringGroupId),
                        Filters.lt(Expense::dueDate.name, cutoffBson)
                    )
                    val updateDoc = Document("\$set", Document(Expense::recurrenceEndDate.name, previousEndDate))
                    expensesCollection.updateMany(updateFilter, updateDoc)
                }

                true
            }

            ExpenseDeleteMode.ALL -> {
                val deleteFilter = Filters.and(
                    Filters.eq(Constants.KEY_WASHER_ID, washerObjectId),
                    Filters.eq(Expense::recurringGroupId.name, recurringGroupId ?: expenseObjectId)
                )
                val deleteUpdate = Document("\$set", Document(Expense::isDeleted.name, true))
                expensesCollection.updateMany(deleteFilter, deleteUpdate).wasAcknowledged()
            }
        }
    }

    override suspend fun materializeRecurringExpenses(washerId: String, targetYearMonth: YearMonth) {
        val washerObjectId = ObjectId(washerId)
        val targetStartInstant = targetYearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val targetStartBson = BsonDateTime(targetStartInstant.toEpochMilli())

        val filter = Filters.and(
            Filters.eq(Constants.KEY_WASHER_ID, washerObjectId),
            Filters.eq(Expense::isRecurringMonthly.name, true),
            Filters.ne(Expense::recurringGroupId.name, null),
            Filters.ne(Expense::isDeleted.name, true),
            Filters.or(
                Filters.eq(Expense::recurrenceEndDate.name, null),
                Filters.gte(Expense::recurrenceEndDate.name, targetStartBson)
            )
        )
        val recurringList = expensesCollection.find(filter).toList()
        if (recurringList.isEmpty()) return

        val groups = recurringList.groupBy { it.recurringGroupId!! }

        for ((groupId, groupExpenses) in groups) {
            val earliestDueDate = groupExpenses.minOf { it.dueDate.value }
            val earliestLocalDate = Instant.ofEpochMilli(earliestDueDate).atZone(ZoneOffset.UTC).toLocalDate()
            val earliestYearMonth = YearMonth.of(earliestLocalDate.year, earliestLocalDate.monthValue)

            if (earliestYearMonth.isAfter(targetYearMonth)) {
                continue
            }

            val template = groupExpenses.maxByOrNull { it.dueDate.value } ?: continue
            val recurrenceDay = template.recurrenceDayOfMonth ?: earliestLocalDate.dayOfMonth

            var currentMonth = earliestYearMonth
            val maxFutureMonth = YearMonth.now(ZoneOffset.UTC).plusMonths(12)
            val effectiveTargetMonth = if (targetYearMonth.isAfter(maxFutureMonth)) maxFutureMonth else targetYearMonth

            while (!currentMonth.isAfter(effectiveTargetMonth)) {
                val currentMonthStart = currentMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                val currentMonthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59).toInstant(ZoneOffset.UTC)

                if (template.recurrenceEndDate != null && currentMonthStart.toEpochMilli() > template.recurrenceEndDate.value) {
                    break
                }

                val monthFilter = Filters.and(
                    Filters.eq(Constants.KEY_WASHER_ID, washerObjectId),
                    Filters.eq(Expense::recurringGroupId.name, groupId),
                    Filters.gte(Expense::dueDate.name, BsonDateTime(currentMonthStart.toEpochMilli())),
                    Filters.lte(Expense::dueDate.name, BsonDateTime(currentMonthEnd.toEpochMilli()))
                )
                val exists = expensesCollection.find(monthFilter).firstOrNull() != null
                if (!exists) {
                    val day = minOf(recurrenceDay, currentMonth.lengthOfMonth())
                    val dueDateInstant = currentMonth.atDay(day).atStartOfDay(ZoneOffset.UTC).toInstant()
                    val newExpense = Expense(
                        washerId = washerObjectId,
                        amount = template.amount,
                        category = template.category,
                        isPaidToday = false,
                        dueDate = BsonDateTime(dueDateInstant.toEpochMilli()),
                        isRecurringMonthly = true,
                        recurringGroupId = groupId,
                        recurrenceDayOfMonth = recurrenceDay,
                        recurrenceEndDate = template.recurrenceEndDate,
                        description = template.description,
                        id = ObjectId()
                    )
                    expensesCollection.insertOne(newExpense)
                }

                currentMonth = currentMonth.plusMonths(1)
            }
        }
    }
}
