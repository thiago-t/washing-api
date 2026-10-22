package com.thiagotoazza.data.source.expense

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.expenses.Expense
import com.thiagotoazza.utils.ApiResult
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.DateFilter
import com.thiagotoazza.utils.toApiResult
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId

class MongoExpenseDataSource(database: MongoDatabase) : ExpenseDataSource {

    private val expensesCollection = database.getCollection<Expense>(Constants.KEY_EXPENSES_COLLECTION)

    override suspend fun getExpenses(washerId: String): List<Expense> {
        val query = Document(Constants.KEY_WASHER_ID, ObjectId(washerId))
        return expensesCollection.find(query)
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
        )
        return expensesCollection.find(filter)
            .sort(Document(Expense::dueDate.name, -1))
            .toList()
    }

    override suspend fun getExpenseById(id: String): Expense? {
        val query = Document("_id", ObjectId(id))
        return expensesCollection.find(query).firstOrNull()
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
        return expensesCollection.deleteOne(query).wasAcknowledged()
    }
}
