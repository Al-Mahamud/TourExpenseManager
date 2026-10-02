package com.tourexpensemanager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tourexpensemanager.ui.AddExpenseScreen
import com.tourexpensemanager.ui.AddMemberScreen
import com.tourexpensemanager.ui.GroupScreen
import com.tourexpensemanager.ui.HomeScreen
import com.tourexpensemanager.ui.ResultScreen
import com.tourexpensemanager.viewmodel.TourViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: TourViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // 1. Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToGroup = { groupId ->
                    navController.navigate(Screen.Group.createRoute(groupId))
                }
            )
        }

        // 2. Group Screen with 3 tabs
        composable(
            route = Screen.Group.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.LongType },
                navArgument("tab") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
            val tab = backStackEntry.arguments?.getInt("tab") ?: 0
            GroupScreen(
                viewModel = viewModel,
                groupId = groupId,
                initialTab = tab,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddExpense = { gId ->
                    navController.navigate(Screen.AddExpense.createRoute(gId))
                },
                onNavigateToEditExpense = { gId, expId ->
                    navController.navigate(Screen.EditExpense.createRoute(gId, expId))
                },
                onNavigateToAddMember = { gId ->
                    navController.navigate(Screen.AddMember.createRoute(gId))
                }
            )
        }

        // 3. Add Member Screen
        composable(
            route = Screen.AddMember.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
            AddMemberScreen(
                viewModel = viewModel,
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 4. Add Expense Screen
        composable(
            route = Screen.AddExpense.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
            AddExpenseScreen(
                viewModel = viewModel,
                groupId = groupId,
                expenseId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5. Edit Expense Screen
        composable(
            route = Screen.EditExpense.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.LongType },
                navArgument("expenseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
            val expenseId = backStackEntry.arguments?.getLong("expenseId")
            AddExpenseScreen(
                viewModel = viewModel,
                groupId = groupId,
                expenseId = expenseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 6. Standalone Result Screen
        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.LongType }
            )
        ) {
            ResultScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
