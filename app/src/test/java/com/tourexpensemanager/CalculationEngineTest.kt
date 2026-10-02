package com.tourexpensemanager

import com.tourexpensemanager.data.ExpenseEntity
import com.tourexpensemanager.data.ExpenseShareEntity
import com.tourexpensemanager.data.MemberEntity
import com.tourexpensemanager.viewmodel.CalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CalculationEngineTest {

    @Test
    fun testEqualShareDistribution_ExactDivision() {
        val memberIds = listOf(1L, 2L, 3L, 4L)
        val shares = CalculationEngine.calculateEqualShares(100.0, memberIds)

        assertEquals(4, shares.size)
        assertEquals(25.0, shares[1L]!!, 0.001)
        assertEquals(25.0, shares[2L]!!, 0.001)
        assertEquals(25.0, shares[3L]!!, 0.001)
        assertEquals(25.0, shares[4L]!!, 0.001)
        assertEquals(100.0, shares.values.sum(), 0.001)
    }

    @Test
    fun testEqualShareDistribution_OddPennies() {
        val memberIds = listOf(1L, 2L, 3L)
        val shares = CalculationEngine.calculateEqualShares(100.0, memberIds)

        assertEquals(3, shares.size)
        // 100 / 3 = 33.33 + 1 penny
        assertEquals(33.34, shares[1L]!!, 0.001)
        assertEquals(33.33, shares[2L]!!, 0.001)
        assertEquals(33.33, shares[3L]!!, 0.001)
        assertEquals(100.0, shares.values.sum(), 0.001)
    }

    @Test
    fun testPlanExampleCalculationAndSettlement() {
        // Al: Paid 6000, Owes 2500, Balance = +3500
        // Rahim: Paid 1000, Owes 2500, Balance = -1500
        // Karim: Paid 3500, Owes 2500, Balance = +1000
        // Hasan: Paid 2000, Owes 5000 (say Hasan's share is higher or debt), Balance = -3000
        // Let's test standard members:
        val members = listOf(
            MemberEntity(id = 1L, groupId = 1L, name = "Al"),
            MemberEntity(id = 2L, groupId = 1L, name = "Rahim"),
            MemberEntity(id = 3L, groupId = 1L, name = "Karim"),
            MemberEntity(id = 4L, groupId = 1L, name = "Hasan")
        )

        // Al paid 6000, Rahim paid 1000, Karim paid 3500, Hasan paid 2000 => Total 12500
        val expenses = listOf(
            ExpenseEntity(id = 1L, groupId = 1L, title = "Hotel", amount = 6000.0, paidBy = 1L),
            ExpenseEntity(id = 2L, groupId = 1L, title = "Dinner", amount = 1000.0, paidBy = 2L),
            ExpenseEntity(id = 3L, groupId = 1L, title = "Transport", amount = 3500.0, paidBy = 3L),
            ExpenseEntity(id = 4L, groupId = 1L, title = "Activities", amount = 2000.0, paidBy = 4L)
        )

        // Equal owes for Hotel: 1500 each
        // Dinner: 250 each
        // Transport: 875 each
        // Activities: 500 each
        // Total owed per member = 1500 + 250 + 875 + 500 = 3125.0
        val shares = mutableListOf<ExpenseShareEntity>()
        for (expense in expenses) {
            val perMember = expense.amount / 4.0
            members.forEach { m ->
                shares.add(ExpenseShareEntity(expenseId = expense.id, memberId = m.id, amount = perMember))
            }
        }

        val summary = CalculationEngine.calculateGroupSummary(members, expenses, shares)
        assertEquals(12500.0, summary.totalExpense, 0.001)

        val alBalance = summary.memberBalances.first { it.memberId == 1L }
        assertEquals(6000.0, alBalance.totalPaid, 0.001)
        assertEquals(3125.0, alBalance.totalOwed, 0.001)
        assertEquals(2875.0, alBalance.balance, 0.001)

        // Check settlements: total transferred must equal total debt
        val totalSettled = summary.settlements.sumOf { it.amount }
        val totalDebt = summary.memberBalances.filter { it.balance < 0 }.sumOf { -it.balance }
        assertEquals(totalDebt, totalSettled, 0.01)

        // Check that every settlement amount > 0
        assertTrue(summary.settlements.all { it.amount > 0.0 })
    }
}
