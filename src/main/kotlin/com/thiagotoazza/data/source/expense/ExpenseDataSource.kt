package com.thiagotoazza.data.source.expense

import com.thiagotoazza.data.models.expenses.Expense
import com.thiagotoazza.utils.ApiResult

interface ExpenseDataSource {
    suspend fun getExpenses(washerId: String): List<Expense>
    suspend fun getExpenseById(id: String): Expense?
    suspend fun insertExpense(expense: Expense): ApiResult
    suspend fun deleteExpense(id: String): Boolean
}
