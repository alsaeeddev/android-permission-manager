package com.example.presentation.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Apps : Screen("apps")
    data object Permissions : Screen("permissions")
    data object Usage : Screen("usage")
    data object Settings : Screen("settings")
    data object AppDetail : Screen("app_detail/{packageName}") {
        fun createRoute(packageName: String): String = "app_detail/$packageName"
    }
    data object SpecialAccess : Screen("special_access/{packageName}") {
        fun createRoute(packageName: String): String = "special_access/$packageName"
    }
}
