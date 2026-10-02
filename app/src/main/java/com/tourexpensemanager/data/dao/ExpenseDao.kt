package com.tourexpensemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.tourexpensemanager.data.ExpenseEntity
import com.tourexpensemanager.data.ExpenseShareEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY date DESC, id DESC")
    fun getExpensesByGroupId(groupId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY date DESC, id DESC")
    suspend fun getExpensesByGroupIdSync(groupId: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    fun getExpenseById(id: Long): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseByIdSync(id: Long): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseShares(shares: List<ExpenseShareEntity>)

    @Query("SELECT * FROM expense_shares WHERE expenseId = :expenseId")
    fun getSharesByExpenseId(expenseId: Long): Flow<List<ExpenseShareEntity>>

    @Query("SELECT * FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun getSharesByExpenseIdSync(expenseId: Long): List<ExpenseShareEntity>

    @Query("DELETE FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun deleteSharesByExpenseId(expenseId: Long)

    @Query(
        """
        SELECT es.* FROM expense_shares es
        INNER JOIN expenses e ON es.expenseId = e.id
        WHERE e.groupId = :groupId
        """
    )
    fun getAllSharesForGroup(groupId: Long): Flow<List<ExpenseShareEntity>>

    @Query(
        """
        SELECT es.* FROM expense_shares es
        INNER JOIN expenses e ON es.expenseId = e.id
        WHERE e.groupId = :groupId
        """
    )
    suspend fun getAllSharesForGroupSync(groupId: Long): List<ExpenseShareEntity>

    @Transaction
    suspend fun insertExpenseWithShares(expense: ExpenseEntity, shares: List<ExpenseShareEntity>): Long {
        val expenseId = insertExpense(expense)
        val sharesWithId = shares.map { it.copy(expenseId = expenseId) }
        insertExpenseShares(sharesWithId)
        return expenseId
    }

    @Transaction
    suspend fun updateExpenseWithShares(expense: ExpenseEntity, shares: List<ExpenseShareEntity>) {
        updateExpense(expense)
        deleteSharesByExpenseId(expense.id)
        val sharesWithId = shares.map { it.copy(expenseId = expense.id) }
        insertExpenseShares(sharesWithId)
    }
}
