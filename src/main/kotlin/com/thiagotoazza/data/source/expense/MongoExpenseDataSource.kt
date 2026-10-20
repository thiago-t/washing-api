package com.thiagotoazza.data.source.expense

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.expenses.Expense
import com.thiagotoazza.utils.ApiResult
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.toApiResult
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId

class MongoExpenseDataSource(database: MongoDatabase) : ExpenseDataSource {

    private val expensesCollection = database.getCollection<Expense>(Constants.KEY_EXPENSES_COLLECTION)

    override suspend fun getExpenses(washerId: String): List<Expense> {
        val query = Document(Constants.KEY_WASHER_ID, ObjectId(washerId))
        return expensesCollection.find(query).toList()
    }

    override suspend fun getExpenseById(id: String): Expense? {
        val query = Document("_id", ObjectId(id))
        return expensesCollection.find(query).firstOrNull()
    }

    override suspend fun insertExpense(expense: Expense): ApiResult {
        return expensesCollection.insertOne(expense).toApiResult()
    }

    override suspend fun deleteExpense(id: String): Boolean {
        val query = Document("_id", ObjectId(id))
        return expensesCollection.deleteOne(query).wasAcknowledged()
    }
}
