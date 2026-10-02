package com.tourexpensemanager.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")

    object Group : Screen("group/{groupId}?tab={tab}") {
        fun createRoute(groupId: Long, tab: Int = 0) = "group/$groupId?tab=$tab"
    }

    object AddMember : Screen("add_member/{groupId}") {
        fun createRoute(groupId: Long) = "add_member/$groupId"
    }

    object AddExpense : Screen("add_expense/{groupId}") {
        fun createRoute(groupId: Long) = "add_expense/$groupId"
    }

    object EditExpense : Screen("edit_expense/{groupId}/{expenseId}") {
        fun createRoute(groupId: Long, expenseId: Long) = "edit_expense/$groupId/$expenseId"
    }

    object Result : Screen("result/{groupId}") {
        fun createRoute(groupId: Long) = "result/$groupId"
    }
}
