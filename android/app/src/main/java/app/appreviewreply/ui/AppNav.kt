package app.appreviewreply.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.appreviewreply.ui.screens.InboxScreen
import app.appreviewreply.ui.screens.IssuesScreen
import app.appreviewreply.ui.screens.ReviewDetailScreen
import app.appreviewreply.ui.screens.SettingsScreen
import app.appreviewreply.ui.screens.SignInScreen

object Routes {
    const val SIGN_IN = "signin"
    const val INBOX = "inbox"
    const val ISSUES = "issues"
    const val SETTINGS = "settings"
    const val REVIEW = "review/{id}"
    fun review(id: String) = "review/$id"
}

@Composable
fun AppNav(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val data by vm.data.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Google consent screen launcher
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.onConsentResult(result.data) else vm.consentDismissed()
    }
    LaunchedEffect(ui.consentIntent) {
        ui.consentIntent?.let { consentLauncher.launch(IntentSenderRequest.Builder(it).build()) }
    }
    val accountLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.onAccountPicked(result.data) else vm.accountPickerDismissed()
    }
    LaunchedEffect(ui.accountPickerIntent) {
        ui.accountPickerIntent?.let { accountLauncher.launch(it) }
    }
    LaunchedEffect(ui.message) {
        ui.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(data.apps.isNotEmpty()) {
        if (data.apps.isNotEmpty() && Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route in setOf(Routes.INBOX, Routes.ISSUES, Routes.SETTINGS)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) NavigationBar {
                NavigationBarItem(selected = route == Routes.INBOX, onClick = { nav.navigate(Routes.INBOX) { launchSingleTop = true; popUpTo(Routes.INBOX) } },
                    icon = { Icon(Icons.Filled.Inbox, null) }, label = { Text("Inbox") })
                NavigationBarItem(selected = route == Routes.ISSUES, onClick = { nav.navigate(Routes.ISSUES) { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.BugReport, null) }, label = { Text("Issues") })
                NavigationBarItem(selected = route == Routes.SETTINGS, onClick = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("Settings") })
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (data.apps.isEmpty()) Routes.SIGN_IN else Routes.INBOX,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SIGN_IN) {
                SignInScreen(vm = vm, onDone = { nav.navigate(Routes.INBOX) { popUpTo(Routes.SIGN_IN) { inclusive = true } } })
            }
            composable(Routes.INBOX) { InboxScreen(vm = vm, onOpen = { nav.navigate(Routes.review(it)) }) }
            composable(Routes.ISSUES) { IssuesScreen(vm = vm, onOpen = { nav.navigate(Routes.review(it)) }) }
            composable(Routes.SETTINGS) { SettingsScreen(vm = vm, onAddApp = { nav.navigate(Routes.SIGN_IN) }) }
            composable(Routes.REVIEW) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                ReviewDetailScreen(vm = vm, reviewId = id, onBack = { nav.popBackStack() })
            }
        }
    }
}
