package com.tourexpensemanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tourexpensemanager.data.MemberEntity
import com.tourexpensemanager.ui.theme.NegativeRed
import com.tourexpensemanager.ui.theme.PositiveGreen
import com.tourexpensemanager.viewmodel.CalculationEngine
import com.tourexpensemanager.viewmodel.TourViewModel
import kotlin.math.abs
import kotlin.math.roundToLong

enum class SplitType {
    EQUAL, CUSTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: TourViewModel,
    groupId: Long,
    expenseId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val members by viewModel.members.collectAsState()
    val expenses by viewModel.expenses.collectAsState()

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedPayerId by remember { mutableStateOf<Long?>(null) }
    var splitType by remember { mutableStateOf(SplitType.EQUAL) }
    val selectedParticipantIds = remember { mutableStateMapOf<Long, Boolean>() }
    val customShares = remember { mutableStateMapOf<Long, String>() }

    var isTitleError by remember { mutableStateOf(false) }
    var isAmountError by remember { mutableStateOf(false) }
    var isPayerError by remember { mutableStateOf(false) }
    var payerDropdownExpanded by remember { mutableStateOf(false) }
    var expenseDate by remember { mutableStateOf(System.currentTimeMillis()) }

    // Initialize if editing existing expense
    LaunchedEffect(expenseId, expenses, members) {
        if (expenseId != null && expenseId > 0) {
            val existing = expenses.find { it.id == expenseId }
            if (existing != null) {
                title = existing.title
                amountText = existing.amount.toString()
                selectedPayerId = existing.paidBy
                expenseDate = existing.date

                val shares = viewModel.getExpenseShares(expenseId)
                selectedParticipantIds.clear()
                customShares.clear()

                // Check if shares are equal or custom
                val count = shares.size
                var areEqual = true
                if (count > 0) {
                    val expectedEqual = CalculationEngine.calculateEqualShares(existing.amount, shares.map { it.memberId })
                    for (s in shares) {
                        selectedParticipantIds[s.memberId] = true
                        customShares[s.memberId] = s.amount.toString()
                        if (abs(s.amount - (expectedEqual[s.memberId] ?: 0.0)) > 0.01) {
                            areEqual = false
                        }
                    }
                }
                splitType = if (areEqual) SplitType.EQUAL else SplitType.CUSTOM
            }
        } else if (members.isNotEmpty() && selectedParticipantIds.isEmpty()) {
            // Default select all members and first member as payer
            if (selectedPayerId == null) {
                selectedPayerId = members.firstOrNull()?.id
            }
            members.forEach { m ->
                selectedParticipantIds[m.id] = true
                customShares[m.id] = ""
            }
        }
    }

    val totalAmount = amountText.toDoubleOrNull() ?: 0.0
    val activeParticipants = members.filter { selectedParticipantIds[it.id] == true }

    // Computed shares
    val computedEqualShares = remember(totalAmount, activeParticipants) {
        CalculationEngine.calculateEqualShares(totalAmount, activeParticipants.map { it.id })
    }

    val customSum = remember(customShares.values.toList(), activeParticipants) {
        activeParticipants.sumOf { m ->
            customShares[m.id]?.toDoubleOrNull() ?: 0.0
        }
    }

    val customDifference = remember(totalAmount, customSum) {
        (totalAmount * 100).roundToLong() - (customSum * 100).roundToLong()
    }

    val isCustomValid = splitType == SplitType.EQUAL || abs(customDifference) == 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (expenseId == null) "Add Expense" else "Edit Expense") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (members.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Members in this Group",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You need to add at least one member to the group before you can record expenses.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go Back to Add Members")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 1. Expense Title
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) isTitleError = false
                    },
                    label = { Text("Expense Title *") },
                    placeholder = { Text("e.g. Hotel, Dinner, Fuel, Transport") },
                    isError = isTitleError,
                    supportingText = {
                        if (isTitleError) {
                            Text("Title is required", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Amount
            item {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if ((it.toDoubleOrNull() ?: 0.0) > 0.0) isAmountError = false
                    },
                    label = { Text("Total Amount *") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null)
                    },
                    isError = isAmountError,
                    supportingText = {
                        if (isAmountError) {
                            Text("Enter a valid amount greater than 0", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Who Paid? (Payer Dropdown)
            item {
                ExposedDropdownMenuBox(
                    expanded = payerDropdownExpanded,
                    onExpandedChange = { payerDropdownExpanded = !payerDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentPayerName = members.find { it.id == selectedPayerId }?.name ?: "Select who paid"
                    OutlinedTextField(
                        value = currentPayerName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid By *") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerDropdownExpanded) },
                        isError = isPayerError,
                        supportingText = {
                            if (isPayerError) {
                                Text("Please select a payer", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = payerDropdownExpanded,
                        onDismissRequest = { payerDropdownExpanded = false }
                    ) {
                        members.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member.name) },
                                onClick = {
                                    selectedPayerId = member.id
                                    isPayerError = false
                                    payerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // 4. Split Type Switcher
            item {
                Text(
                    text = "Split Type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = splitType == SplitType.EQUAL,
                        onClick = { splitType = SplitType.EQUAL },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Equal Split")
                    }
                    SegmentedButton(
                        selected = splitType == SplitType.CUSTOM,
                        onClick = {
                            splitType = SplitType.CUSTOM
                            // Prefill custom shares with equal split if empty
                            if (totalAmount > 0 && activeParticipants.isNotEmpty()) {
                                activeParticipants.forEach { m ->
                                    if (customShares[m.id].isNullOrBlank()) {
                                        val eq = computedEqualShares[m.id] ?: 0.0
                                        customShares[m.id] = CalculationEngine.formatCurrency(eq)
                                    }
                                }
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Custom Split")
                    }
                }
            }

            // 5. Participants Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Participants (${activeParticipants.size}/${members.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        TextButton(
                            onClick = {
                                members.forEach { selectedParticipantIds[it.id] = true }
                            }
                        ) {
                            Text("Select All", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = {
                                members.forEach { selectedParticipantIds[it.id] = false }
                            }
                        ) {
                            Text("Deselect All", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Custom Split Helper & Validation Card
            if (splitType == SplitType.CUSTOM) {
                item {
                    val diffCents = customDifference
                    val diffAmount = abs(diffCents) / 100.0
                    val isExact = diffCents == 0L

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExact) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Expense: $${CalculationEngine.formatCurrency(totalAmount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Allocated: $${CalculationEngine.formatCurrency(customSum)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isExact) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PositiveGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Shares match the total expense amount perfectly.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PositiveGreen
                                    )
                                }
                            } else if (diffCents > 0) {
                                Text(
                                    text = "Remaining to allocate: $${CalculationEngine.formatCurrency(diffAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(
                                    text = "Over-allocated by: $${CalculationEngine.formatCurrency(diffAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Participants List
            items(members, key = { it.id }) { member ->
                val isSelected = selectedParticipantIds[member.id] == true

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 1.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                selectedParticipantIds[member.id] = checked
                                if (checked && customShares[member.id].isNullOrBlank() && totalAmount > 0) {
                                    customShares[member.id] = CalculationEngine.formatCurrency(
                                        computedEqualShares[member.id] ?: 0.0
                                    )
                                }
                            }
                        )

                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            if (splitType == SplitType.EQUAL) {
                                val shareVal = computedEqualShares[member.id] ?: 0.0
                                Text(
                                    text = "$${CalculationEngine.formatCurrency(shareVal)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            } else {
                                // Custom Split Input
                                OutlinedTextField(
                                    value = customShares[member.id] ?: "",
                                    onValueChange = { newValue ->
                                        customShares[member.id] = newValue
                                    },
                                    placeholder = { Text("0.00") },
                                    leadingIcon = {
                                        Text("$", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(110.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Save / Submit Button
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val validTitle = title.isNotBlank()
                        val validAmount = totalAmount > 0.0
                        val validPayer = selectedPayerId != null && selectedPayerId!! > 0L
                        val validParticipants = activeParticipants.isNotEmpty()

                        isTitleError = !validTitle
                        isAmountError = !validAmount
                        isPayerError = !validPayer

                        if (!validTitle || !validAmount || !validPayer || !validParticipants || !isCustomValid) {
                            return@Button
                        }

                        // Build shares map
                        val finalShares = mutableMapOf<Long, Double>()
                        if (splitType == SplitType.EQUAL) {
                            finalShares.putAll(computedEqualShares)
                        } else {
                            for (p in activeParticipants) {
                                val shareAmount = customShares[p.id]?.toDoubleOrNull() ?: 0.0
                                finalShares[p.id] = (shareAmount * 100).roundToLong() / 100.0
                            }
                        }

                        if (expenseId == null) {
                            viewModel.addExpense(
                                groupId = groupId,
                                title = title.trim(),
                                amount = totalAmount,
                                paidBy = selectedPayerId!!,
                                shares = finalShares,
                                onSuccess = onNavigateBack
                            )
                        } else {
                            viewModel.editExpense(
                                expenseId = expenseId,
                                groupId = groupId,
                                title = title.trim(),
                                amount = totalAmount,
                                paidBy = selectedPayerId!!,
                                shares = finalShares,
                                date = expenseDate,
                                onSuccess = onNavigateBack
                            )
                        }
                    },
                    enabled = isCustomValid && activeParticipants.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = if (expenseId == null) "Save Expense" else "Update Expense",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        }
    }
}

