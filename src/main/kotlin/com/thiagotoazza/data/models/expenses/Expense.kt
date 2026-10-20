package com.thiagotoazza.data.models.expenses

import com.thiagotoazza.utils.asDecimalString
import org.bson.BsonDateTime
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Expense(
    val washerId: ObjectId,
    val amount: Int,
    val category: ExpenseCategory,
    val isPaidToday: Boolean,
    val dueDate: BsonDateTime,
    val isRecurringMonthly: Boolean,
    val description: String? = null,
    @BsonId val id: ObjectId = ObjectId(),
)

data class ExpenseRequest(
    val amount: Int,
    val category: ExpenseCategory,
    val isPaidToday: Boolean,
    val dueDate: Long,
    val isRecurringMonthly: Boolean,
    val description: String? = null,
)

data class ExpenseResponse(
    val id: String,
    val washerId: String,
    val amount: String,
    val category: String,
    val isPaidToday: Boolean,
    val dueDate: String,
    val isRecurringMonthly: Boolean,
    val description: String? = null,
)

fun Expense.toExpenseResponse() = ExpenseResponse(
    id = id.toString(),
    washerId = washerId.toString(),
    amount = amount.asDecimalString(),
    category = category.name,
    isPaidToday = isPaidToday,
    dueDate = dueDate.value.toString(),
    isRecurringMonthly = isRecurringMonthly,
    description = description
)
