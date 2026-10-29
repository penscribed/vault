package com.kingsman.vault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kingsman.vault.MainActivity
import com.kingsman.vault.ui.screens.AddEntryScreen
import com.kingsman.vault.ui.screens.BackupScreen
import com.kingsman.vault.ui.screens.BankAlertsScreen
import com.kingsman.vault.ui.screens.BudgetsScreen
import com.kingsman.vault.ui.screens.LegalDoc
import com.kingsman.vault.ui.screens.LegalScreen
import com.kingsman.vault.ui.screens.UpdatesScreen
import com.kingsman.vault.ui.screens.BusinessesScreen
import com.kingsman.vault.ui.screens.CategoriesScreen
import com.kingsman.vault.ui.screens.HomeScreen
import com.kingsman.vault.ui.screens.MoreScreen
import com.kingsman.vault.ui.screens.RecordsScreen
import com.kingsman.vault.ui.screens.ReportsScreen
import com.kingsman.vault.ui.screens.SecurityScreen
import com.kingsman.vault.ui.screens.SubscriptionsScreen
import com.kingsman.vault.ui.screens.WealthScreen
import com.kingsman.vault.ui.theme.LocalVault

object Routes {
    const val HOME = "home"
    const val RECORDS = "records"
    const val WEALTH = "wealth"
    const val REPORTS = "reports"
    const val MORE = "more"
    const val ADD = "add?txnId={txnId}&recurringId={recurringId}&suggestionId={suggestionId}"
    const val SUBSCRIPTIONS = "subscriptions"
    const val BUSINESSES = "businesses"
    const val CATEGORIES = "categories"
    const val SECURITY = "security"
    const val BACKUP = "backup"
    const val BUDGETS = com.kingsman.vault.data.ROUTE_BUDGETS
    const val BANK = com.kingsman.vault.data.ROUTE_BANK
    const val UPDATES = com.kingsman.vault.update.ROUTE_UPDATES
    const val LEGAL = "legal/{doc}"
    fun legal(doc: String) = "legal/$doc"

    fun add(txnId: Long = -1L, recurringId: Long = -1L, suggestionId: Long = -1L) =
        "add?txnId=$txnId&recurringId=$recurringId&suggestionId=$suggestionId"
    val tabs = setOf(HOME, RECORDS, WEALTH, REPORTS, MORE)
}

@Composable
fun VaultRoot(vm: VaultViewModel, activity: MainActivity) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = Routes.HOME, modifier = Modifier.fillMaxSize()) {
            composable(Routes.HOME) { HomeScreen(vm, nav) }
            composable(Routes.RECORDS) { RecordsScreen(vm, nav) }
            composable(Routes.WEALTH) { WealthScreen(vm) }
            composable(Routes.REPORTS) { ReportsScreen(vm, activity) }
            composable(Routes.MORE) { MoreScreen(vm, nav, activity) }
            composable(
                Routes.ADD,
                arguments = listOf(
                    navArgument("txnId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("recurringId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("suggestionId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { e ->
                AddEntryScreen(
                    vm = vm,
                    txnId = e.arguments?.getLong("txnId") ?: -1L,
                    recurringId = e.arguments?.getLong("recurringId") ?: -1L,
                    suggestionId = e.arguments?.getLong("suggestionId") ?: -1L,
                    activity = activity,
                    onDone = { nav.popBackStack() }
                )
            }
            composable(Routes.SUBSCRIPTIONS) { SubscriptionsScreen(vm) { nav.popBackStack() } }
            composable(Routes.BUSINESSES) { BusinessesScreen(vm) { nav.popBackStack() } }
            composable(Routes.CATEGORIES) { CategoriesScreen(vm) { nav.popBackStack() } }
            composable(Routes.SECURITY) { SecurityScreen(vm, activity) { nav.popBackStack() } }
            composable(Routes.BACKUP) { BackupScreen(vm, activity) { nav.popBackStack() } }
            composable(Routes.BUDGETS) { BudgetsScreen(vm, activity) { nav.popBackStack() } }
            composable(Routes.BANK) { BankAlertsScreen(vm, activity, nav) { nav.popBackStack() } }
            composable(Routes.UPDATES) { UpdatesScreen(vm, activity) { nav.popBackStack() } }
            composable(Routes.LEGAL, arguments = listOf(navArgument("doc") { type = NavType.StringType })) { e ->
                LegalScreen(e.arguments?.getString("doc") ?: LegalDoc.PRIVACY) { nav.popBackStack() }
            }
        }

        // Open a screen requested by a notification or a shared bank alert
        val pending by activity.pendingRoute.collectAsState()
        LaunchedEffect(pending) {
            pending?.let { route ->
                activity.pendingRoute.value = null
                runCatching { nav.navigate(route) { launchSingleTop = true } }
            }
        }
        if (route != null && route in Routes.tabs) {
            BottomBar(nav, route, Modifier.align(Alignment.BottomCenter))
        }
    }
}

fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(nav: NavHostController, current: String?, modifier: Modifier) {
    val c = LocalVault.current
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.line, shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabIcon(Icons.Rounded.Home, "Home", current == Routes.HOME) { nav.goTab(Routes.HOME) }
        TabIcon(Icons.Rounded.Receipt, "Records", current == Routes.RECORDS) { nav.goTab(Routes.RECORDS) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.acc)
                    .clickable(role = Role.Button, onClickLabel = "Add entry") { nav.navigate(Routes.add()) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add entry", tint = c.accInk)
            }
        }
        TabIcon(Icons.Rounded.Savings, "Wealth", current == Routes.WEALTH) { nav.goTab(Routes.WEALTH) }
        TabIcon(Icons.Rounded.BarChart, "Reports", current == Routes.REPORTS) { nav.goTab(Routes.REPORTS) }
        TabIcon(Icons.Rounded.MoreHoriz, "More", current == Routes.MORE) { nav.goTab(Routes.MORE) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val c = LocalVault.current
    Box(
        Modifier
            .weight(1f)
            .height(64.dp)
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) c.accText else c.muted)
    }
}
