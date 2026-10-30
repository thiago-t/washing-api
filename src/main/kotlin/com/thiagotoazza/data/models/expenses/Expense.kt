package com.thiagotoazza.data.models.expenses

import com.thiagotoazza.utils.asDecimalString
import org.bson.BsonDateTime
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Expense(
    val washerId: ObjectId = ObjectId(),
    val amount: Int = 0,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val isPaidToday: Boolean? = false,
    val dueDate: BsonDateTime = BsonDateTime(0),
    val isRecurringMonthly: Boolean? = false,
    val recurringGroupId: ObjectId? = null,
    val recurrenceDayOfMonth: Int? = null,
    val recurrenceEndDate: BsonDateTime? = null,
    val description: String? = null,
    val isDeleted: Boolean? = false,
    @BsonId val id: ObjectId = ObjectId(),
)

data class ExpenseRequest(
    val amount: Int,
    val category: ExpenseCategory,
    val isPaidToday: Boolean,
    val dueDate: Long,
    val isRecurringMonthly: Boolean,
    val recurringGroupId: String? = null,
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
    val recurringGroupId: String? = null,
    val description: String? = null,
)

fun Expense.toExpenseResponse() = ExpenseResponse(
    id = id.toString(),
    washerId = washerId.toString(),
    amount = amount.asDecimalString(),
    category = category.name,
    isPaidToday = isPaidToday == true,
    dueDate = dueDate.value.toString(),
    isRecurringMonthly = isRecurringMonthly == true,
    recurringGroupId = recurringGroupId?.toString(),
    description = description
)

enum class ExpenseDeleteMode {
    THIS_ONLY,
    THIS_AND_FUTURE,
    ALL;

    companion object {
        fun fromString(value: String?): ExpenseDeleteMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: THIS_ONLY
        }
    }
}
