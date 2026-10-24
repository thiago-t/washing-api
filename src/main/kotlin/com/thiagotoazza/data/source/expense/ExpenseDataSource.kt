package com.thiagotoazza.data.source.expense

import com.thiagotoazza.data.models.expenses.Expense
import com.thiagotoazza.data.models.expenses.ExpenseDeleteMode
import com.thiagotoazza.utils.ApiResult
import com.thiagotoazza.utils.DateFilter
import java.time.YearMonth

interface ExpenseDataSource {
    suspend fun getExpenses(washerId: String): List<Expense>
    suspend fun getExpensesByDate(washerId: String, dateFilter: DateFilter): List<Expense>
    suspend fun getExpenseById(id: String): Expense?
    suspend fun insertExpense(expense: Expense): ApiResult
    suspend fun updateExpense(expense: Expense): Boolean
    suspend fun deleteExpense(id: String): Boolean
    suspend fun deleteExpenseWithMode(washerId: String, id: String, mode: ExpenseDeleteMode): Boolean
    suspend fun materializeRecurringExpenses(washerId: String, targetYearMonth: YearMonth)
}
