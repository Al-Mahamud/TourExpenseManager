package com.tourexpensemanager.viewmodel

import com.tourexpensemanager.data.ExpenseEntity
import com.tourexpensemanager.data.ExpenseShareEntity
import com.tourexpensemanager.data.MemberEntity
import kotlin.math.abs
import kotlin.math.roundToLong

data class MemberBalance(
    val memberId: Long,
    val memberName: String,
    val totalPaid: Double,
    val totalOwed: Double,
    val balance: Double
)

data class SettlementTransaction(
    val fromMemberId: Long,
    val fromMemberName: String,
    val toMemberId: Long,
    val toMemberName: String,
    val amount: Double
)

data class GroupSummary(
    val totalExpense: Double,
    val memberBalances: List<MemberBalance>,
    val settlements: List<SettlementTransaction>
)

object CalculationEngine {

    /**
     * Splits an amount equally among participants, allocating any leftover pennies
     * so the sum of shares always matches the total amount exactly.
     */
    fun calculateEqualShares(totalAmount: Double, memberIds: List<Long>): Map<Long, Double> {
        if (memberIds.isEmpty() || totalAmount <= 0.0) return emptyMap()

        val totalCents = (totalAmount * 100).roundToLong()
        val count = memberIds.size
        val baseCents = totalCents / count
        var remainderCents = totalCents % count

        val result = mutableMapOf<Long, Double>()
        for (memberId in memberIds) {
            val shareCents = if (remainderCents > 0) {
                remainderCents--
                baseCents + 1
            } else {
                baseCents
            }
            result[memberId] = shareCents / 100.0
        }
        return result
    }

    /**
     * Calculates total paid, total owed, and balance for each member in a group,
     * and generates the optimal who-pays-whom settlement transactions.
     */
    fun calculateGroupSummary(
        members: List<MemberEntity>,
        expenses: List<ExpenseEntity>,
        shares: List<ExpenseShareEntity>
    ): GroupSummary {
        val totalExpense = expenses.sumOf { it.amount }
        val memberMap = members.associateBy { it.id }

        // 1. Calculate totalPaid per member (sum of expenses where member is payer)
        val paidMap = mutableMapOf<Long, Double>()
        for (expense in expenses) {
            paidMap[expense.paidBy] = (paidMap[expense.paidBy] ?: 0.0) + expense.amount
        }

        // 2. Calculate totalOwed per member (sum of ExpenseShare amounts for that member)
        val owedMap = mutableMapOf<Long, Double>()
        for (share in shares) {
            owedMap[share.memberId] = (owedMap[share.memberId] ?: 0.0) + share.amount
        }

        // 3. Balance = totalPaid - totalOwed
        val memberBalances = members.map { member ->
            val paid = roundToTwoDecimals(paidMap[member.id] ?: 0.0)
            val owed = roundToTwoDecimals(owedMap[member.id] ?: 0.0)
            val balance = roundToTwoDecimals(paid - owed)
            MemberBalance(
                memberId = member.id,
                memberName = member.name,
                totalPaid = paid,
                totalOwed = owed,
                balance = balance
            )
        }

        // 4. Calculate Settlement using greedy two-pointer approach
        val settlements = calculateSettlements(memberBalances)

        return GroupSummary(
            totalExpense = roundToTwoDecimals(totalExpense),
            memberBalances = memberBalances,
            settlements = settlements
        )
    }

    /**
     * Greedy two-pointer settlement algorithm:
     * Separate members into receivers (balance > 0) and payers (balance < 0).
     * Match a payer with a receiver, transfer the smaller of the two balances,
     * reduce both balances, and continue until all balances are settled.
     */
    fun calculateSettlements(memberBalances: List<MemberBalance>): List<SettlementTransaction> {
        val settlements = mutableListOf<SettlementTransaction>()

        class Party(val id: Long, val name: String, var amountCents: Long)

        val receivers = memberBalances
            .filter { it.balance > 0.001 }
            .map { Party(it.memberId, it.memberName, (it.balance * 100).roundToLong()) }
            .sortedByDescending { it.amountCents }
            .toMutableList()

        val payers = memberBalances
            .filter { it.balance < -0.001 }
            .map { Party(it.memberId, it.memberName, (-it.balance * 100).roundToLong()) }
            .sortedByDescending { it.amountCents }
            .toMutableList()

        var receiverIdx = 0
        var payerIdx = 0

        while (receiverIdx < receivers.size && payerIdx < payers.size) {
            val receiver = receivers[receiverIdx]
            val payer = payers[payerIdx]

            val settleCents = minOf(payer.amountCents, receiver.amountCents)
            if (settleCents > 0) {
                settlements.add(
                    SettlementTransaction(
                        fromMemberId = payer.id,
                        fromMemberName = payer.name,
                        toMemberId = receiver.id,
                        toMemberName = receiver.name,
                        amount = settleCents / 100.0
                    )
                )

                payer.amountCents -= settleCents
                receiver.amountCents -= settleCents
            }

            if (receiver.amountCents == 0L) {
                receiverIdx++
            }
            if (payer.amountCents == 0L) {
                payerIdx++
            }
        }

        return settlements
    }

    fun roundToTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToLong() / 100.0
    }

    fun formatCurrency(amount: Double): String {
        return String.format("%.2f", amount)
    }
}
