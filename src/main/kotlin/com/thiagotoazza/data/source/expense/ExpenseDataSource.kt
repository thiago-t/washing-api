package com.thiagotoazza.data.source.expense

import com.thiagotoazza.data.models.expenses.Expense
import com.thiagotoazza.utils.ApiResult
import com.thiagotoazza.utils.DateFilter

interface ExpenseDataSource {
    suspend fun getExpenses(washerId: String): List<Expense>
    suspend fun getExpensesByDate(washerId: String, dateFilter: DateFilter): List<Expense>
    suspend fun getExpenseById(id: String): Expense?
    suspend fun insertExpense(expense: Expense): ApiResult
    suspend fun updateExpense(expense: Expense): Boolean
    suspend fun deleteExpense(id: String): Boolean
}
