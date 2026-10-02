package com.tourexpensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tourexpensemanager.data.AppDatabase
import com.tourexpensemanager.data.ExpenseEntity
import com.tourexpensemanager.data.ExpenseShareEntity
import com.tourexpensemanager.data.GroupEntity
import com.tourexpensemanager.data.MemberEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailedExpense(
    val id: Long,
    val groupId: Long,
    val title: String,
    val amount: Double,
    val paidById: Long,
    val paidByName: String,
    val date: Long,
    val shares: List<DetailedShare>
)

data class DetailedShare(
    val memberId: Long,
    val memberName: String,
    val amount: Double
)

class TourViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val groupDao = db.groupDao()
    private val memberDao = db.memberDao()
    private val expenseDao = db.expenseDao()

    // 1. All Groups Flow
    val groups: StateFlow<List<GroupEntity>> = groupDao.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Currently Selected Group ID
    private val _selectedGroupId = MutableStateFlow<Long?>(null)
    val selectedGroupId: StateFlow<Long?> = _selectedGroupId.asStateFlow()

    fun selectGroup(groupId: Long) {
        _selectedGroupId.value = groupId
    }

    // 3. Current Group Details
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedGroup: StateFlow<GroupEntity?> = _selectedGroupId.flatMapLatest { id ->
        if (id != null) groupDao.getGroupById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 4. Members for current group
    @OptIn(ExperimentalCoroutinesApi::class)
    val members: StateFlow<List<MemberEntity>> = _selectedGroupId.flatMapLatest { id ->
        if (id != null) memberDao.getMembersByGroupId(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Expenses for current group
    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: StateFlow<List<ExpenseEntity>> = _selectedGroupId.flatMapLatest { id ->
        if (id != null) expenseDao.getExpensesByGroupId(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. Expense Shares for current group
    @OptIn(ExperimentalCoroutinesApi::class)
    val shares: StateFlow<List<ExpenseShareEntity>> = _selectedGroupId.flatMapLatest { id ->
        if (id != null) expenseDao.getAllSharesForGroup(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7. Detailed Expenses combining expense + payer + shares
    val detailedExpenses: StateFlow<List<DetailedExpense>> = combine(
        expenses,
        members,
        shares
    ) { expenseList, memberList, shareList ->
        val memberMap = memberList.associateBy { it.id }
        val shareMap = shareList.groupBy { it.expenseId }

        expenseList.map { exp ->
            val payerName = memberMap[exp.paidBy]?.name ?: "Unknown"
            val expShares = (shareMap[exp.id] ?: emptyList()).map { s ->
                DetailedShare(
                    memberId = s.memberId,
                    memberName = memberMap[s.memberId]?.name ?: "Unknown",
                    amount = s.amount
                )
            }
            DetailedExpense(
                id = exp.id,
                groupId = exp.groupId,
                title = exp.title,
                amount = exp.amount,
                paidById = exp.paidBy,
                paidByName = payerName,
                date = exp.date,
                shares = expShares
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 8. Group Summary & Settlement
    val groupSummary: StateFlow<GroupSummary> = combine(
        members,
        expenses,
        shares
    ) { memberList, expenseList, shareList ->
        CalculationEngine.calculateGroupSummary(memberList, expenseList, shareList)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GroupSummary(0.0, emptyList(), emptyList())
    )

    // --- Group Actions ---

    fun createGroup(name: String, initialMemberNames: List<String> = emptyList(), onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val groupId = groupDao.insertGroup(GroupEntity(name = name.trim()))
            val membersToInsert = initialMemberNames
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { MemberEntity(groupId = groupId, name = it) }
            if (membersToInsert.isNotEmpty()) {
                memberDao.insertMembers(membersToInsert)
            }
            _selectedGroupId.value = groupId
            onCreated(groupId)
        }
    }

    fun deleteGroup(groupId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            groupDao.deleteGroupById(groupId)
            if (_selectedGroupId.value == groupId) {
                _selectedGroupId.value = null
            }
            onDeleted()
        }
    }

    // --- Member Actions ---

    fun addMember(groupId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            memberDao.insertMember(MemberEntity(groupId = groupId, name = name.trim()))
        }
    }

    fun editMember(memberId: Long, groupId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            memberDao.updateMember(MemberEntity(id = memberId, groupId = groupId, name = newName.trim()))
        }
    }

    fun deleteMember(memberId: Long) {
        viewModelScope.launch {
            memberDao.deleteMemberById(memberId)
        }
    }

    // --- Expense Actions ---

    fun addExpense(
        groupId: Long,
        title: String,
        amount: Double,
        paidBy: Long,
        shares: Map<Long, Double>,
        onSuccess: () -> Unit = {}
    ) {
        if (title.isBlank() || amount <= 0.0 || paidBy <= 0L || shares.isEmpty()) return
        viewModelScope.launch {
            val expense = ExpenseEntity(
                groupId = groupId,
                title = title.trim(),
                amount = amount,
                paidBy = paidBy,
                date = System.currentTimeMillis()
            )
            val shareEntities = shares.map { (mId, mAmount) ->
                ExpenseShareEntity(expenseId = 0, memberId = mId, amount = mAmount)
            }
            expenseDao.insertExpenseWithShares(expense, shareEntities)
            onSuccess()
        }
    }

    fun editExpense(
        expenseId: Long,
        groupId: Long,
        title: String,
        amount: Double,
        paidBy: Long,
        shares: Map<Long, Double>,
        date: Long,
        onSuccess: () -> Unit = {}
    ) {
        if (title.isBlank() || amount <= 0.0 || paidBy <= 0L || shares.isEmpty()) return
        viewModelScope.launch {
            val expense = ExpenseEntity(
                id = expenseId,
                groupId = groupId,
                title = title.trim(),
                amount = amount,
                paidBy = paidBy,
                date = date
            )
            val shareEntities = shares.map { (mId, mAmount) ->
                ExpenseShareEntity(expenseId = expenseId, memberId = mId, amount = mAmount)
            }
            expenseDao.updateExpenseWithShares(expense, shareEntities)
            onSuccess()
        }
    }

    fun deleteExpense(expenseId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            expenseDao.deleteExpenseById(expenseId)
            onSuccess()
        }
    }

    suspend fun getExpenseShares(expenseId: Long): List<ExpenseShareEntity> {
        return expenseDao.getSharesByExpenseIdSync(expenseId)
    }
}
