package com.example.presentation

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.PermoraApplication
import com.example.core.PermissionGroupType
import com.example.presentation.appdetail.AppDetailScreen
import com.example.presentation.appdetail.AppDetailViewModel
import com.example.presentation.applist.AppListScreen
import com.example.presentation.applist.AppListViewModel
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.dashboard.DashboardViewModel
import com.example.presentation.navigation.Screen
import com.example.presentation.permissioncentric.PermissionCentricScreen
import com.example.presentation.permissioncentric.PermissionCentricViewModel
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.settings.SettingsViewModel
import com.example.presentation.specialaccess.SpecialAccessScreen
import com.example.presentation.specialaccess.SpecialAccessViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PermoraApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val app = LocalContext.current.applicationContext as PermoraApplication
    val container = app.container

    val bottomNavRoutes = listOf(
        Screen.Dashboard.route,
        Screen.Apps.route,
        Screen.Permissions.route,
        Screen.Usage.route,
        Screen.Settings.route
    )

    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = showBottomBar) {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Dashboard.route,
                        onClick = {
                            navController.navigateToTab(Screen.Dashboard.route)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("nav_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Apps.route,
                        onClick = {
                            navController.navigateToTab(Screen.Apps.route)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = "Apps"
                            )
                        },
                        label = { Text("Apps") },
                        modifier = Modifier.testTag("nav_apps")
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Permissions.route,
                        onClick = {
                            navController.navigateToTab(Screen.Permissions.route)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Permissions"
                            )
                        },
                        label = { Text("Permissions") },
                        modifier = Modifier.testTag("nav_permissions")
                    )

                    /*                    NavigationBarItem(
                                            selected = currentRoute == Screen.Usage.route,
                                            onClick = {
                                                navController.navigateToTab(Screen.Usage.route)
                                            },
                                            icon = { Icon(imageVector = Icons.Default.History, contentDescription = "Usage") },
                                            label = { Text("Usage") },
                                            modifier = Modifier.testTag("nav_usage")
                                        )*/

                    NavigationBarItem(
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            navController.navigateToTab(Screen.Settings.route)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            // Dashboard
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.Factory(
                        container.getInstalledAppsUseCase,
                        container.permissionController
                    )
                )
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToApps = {
                        navController.navigateToTab(Screen.Apps.route)
                    },
                    onNavigateToAppDetail = { packageName ->
                        navController.navigate(Screen.AppDetail.createRoute(packageName))
                    },
                    onNavigateToPermissionGroup = { group ->
                        navController.navigateToTab(Screen.Permissions.route)
                    },
                    onOpenShizukuGuide = {
                        navController.navigateToTab(Screen.Settings.route)
                    }
                )
            }

            // Apps List
            composable(Screen.Apps.route) {
                val viewModel: AppListViewModel = viewModel(
                    factory = AppListViewModel.Factory(
                        container.getInstalledAppsUseCase,
                        container.searchFilterUseCase,
                        container.revokeAllPermissionsUseCase
                    )
                )
                AppListScreen(
                    viewModel = viewModel,
                    onNavigateToAppDetail = { packageName ->
                        navController.navigate(Screen.AppDetail.createRoute(packageName))
                    }
                )
            }

            // Permissions centric
            composable(Screen.Permissions.route) {
                val viewModel: PermissionCentricViewModel = viewModel(
                    factory = PermissionCentricViewModel.Factory(
                        initialGroup = PermissionGroupType.CAMERA,
                        getAppsByPermissionUseCase = container.getAppsByPermissionUseCase,
                        setPermissionStateUseCase = container.setPermissionStateUseCase,
                        revokeAllPermissionsUseCase = container.revokeAllPermissionsUseCase,
                        permissionController = container.permissionController
                    )
                )
                PermissionCentricScreen(
                    viewModel = viewModel,
                    onNavigateToAppDetail = { packageName ->
                        navController.navigate(Screen.AppDetail.createRoute(packageName))
                    }
                )
            }

            // Usage Audit
            composable(Screen.Usage.route) {
                val viewModel: com.example.presentation.usage.UsageViewModel = viewModel(
                    factory = com.example.presentation.usage.UsageViewModel.Factory(
                        container.getPermissionUsageUseCase
                    )
                )
                com.example.presentation.usage.UsageScreen(
                    viewModel = viewModel,
                    onNavigateToAppDetail = { packageName ->
                        navController.navigate(Screen.AppDetail.createRoute(packageName))
                    }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        settingsRepository = container.settingsRepository,
                        permissionController = container.permissionController,
                        getInstalledAppsUseCase = container.getInstalledAppsUseCase,
                        appPermissionRepository = container.appPermissionRepository,
                        exportReportUseCase = container.exportReportUseCase
                    )
                )
                SettingsScreen(viewModel = viewModel)
            }

            // App Detail
            composable(
                route = Screen.AppDetail.route,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType })
            ) { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                val viewModel: AppDetailViewModel = viewModel(
                    key = packageName,
                    factory = AppDetailViewModel.Factory(
                        packageName = packageName,
                        getAppPermissionsUseCase = container.getAppPermissionsUseCase,
                        setPermissionStateUseCase = container.setPermissionStateUseCase,
                        revokeAllPermissionsUseCase = container.revokeAllPermissionsUseCase,
                        undoRevokeUseCase = container.undoRevokeUseCase,
                        permissionController = container.permissionController
                    )
                )
                AppDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSpecialAccess = { pkg ->
                        navController.navigate(Screen.SpecialAccess.createRoute(pkg))
                    }
                )
            }

            // Special Access
            composable(
                route = Screen.SpecialAccess.route,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType })
            ) { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                val viewModel: SpecialAccessViewModel = viewModel(
                    key = packageName,
                    factory = SpecialAccessViewModel.Factory(
                        packageName = packageName,
                        getAppPermissionsUseCase = container.getAppPermissionsUseCase,
                        permissionController = container.permissionController
                    )
                )
                SpecialAccessScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

