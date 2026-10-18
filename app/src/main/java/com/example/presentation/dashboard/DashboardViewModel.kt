package com.example.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.PermissionGroupType
import com.example.core.RiskLevel
import com.example.domain.model.AppInfo
import com.example.domain.model.ControllerMode
import com.example.domain.model.ModeStatus
import com.example.domain.repository.PermissionController
import com.example.domain.usecase.GetInstalledAppsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val totalApps: Int = 0,
    val userAppsCount: Int = 0,
    val systemAppsCount: Int = 0,
    val overallPrivacyScore: Int = 100,
    val sensitiveCounts: Map<PermissionGroupType, Int> = emptyMap(),
    val riskyApps: List<AppInfo> = emptyList(),
    val modeStatus: ModeStatus = ModeStatus(ControllerMode.SETTINGS_REDIRECT, false, false, ""),
    val lastUpdatedTime: Long = 0L
)

class DashboardViewModel(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val permissionController: PermissionController
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val modeStatusState = MutableStateFlow(permissionController.getModeStatus())

    val uiState: StateFlow<DashboardUiState> = combine(
        getInstalledAppsUseCase(),
        isRefreshing,
        modeStatusState
    ) { apps, refreshing, mode ->
        if (apps.isEmpty() && refreshing) {
            DashboardUiState(isLoading = true, modeStatus = mode)
        } else {
            val userApps = apps.count { !it.isSystemApp }
            val systemApps = apps.count { it.isSystemApp }

            val counts = mutableMapOf<PermissionGroupType, Int>()
            val sensitiveGroups = listOf(
                PermissionGroupType.LOCATION,
                PermissionGroupType.CAMERA,
                PermissionGroupType.MICROPHONE,
                PermissionGroupType.CONTACTS,
                PermissionGroupType.SMS,
                PermissionGroupType.CALL_LOG,
                PermissionGroupType.STORAGE,
                PermissionGroupType.PHONE,
                PermissionGroupType.CALENDAR,
                PermissionGroupType.SENSORS,
                PermissionGroupType.NEARBY_DEVICES,
                PermissionGroupType.NOTIFICATIONS
            )

            for (group in sensitiveGroups) {
                counts[group] = apps.count { app -> app.sensitiveGroups.contains(group) }
            }

            val evaluatedApps = apps.filter { !it.isSystemApp }
            val averageRisk = if (evaluatedApps.isNotEmpty()) {
                evaluatedApps.map { it.riskScore }.average().toInt()
            } else 0
            val privacyScore = (100 - averageRisk).coerceIn(0, 100)

            val risky = apps
                .filter { it.riskLevel == RiskLevel.CRITICAL || it.riskLevel == RiskLevel.HIGH }
                .sortedByDescending { it.riskScore }
                .take(6)

            val lastUpdated = apps.maxOfOrNull { it.lastCheckedAt } ?: 0L

            DashboardUiState(
                isLoading = false,
                isRefreshing = refreshing,
                totalApps = apps.size,
                userAppsCount = userApps,
                systemAppsCount = systemApps,
                overallPrivacyScore = privacyScore,
                sensitiveCounts = counts,
                riskyApps = risky,
                modeStatus = mode,
                lastUpdatedTime = lastUpdated
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    init {
        refresh(force = false)
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            if (force) {
                isRefreshing.value = true
            }
            modeStatusState.value = permissionController.getModeStatus()
            getInstalledAppsUseCase.refresh(force = force)
            isRefreshing.value = false
        }
    }

    fun requestShizukuPermission() {
        permissionController.requestShizukuPermission()
        modeStatusState.value = permissionController.getModeStatus()
    }

    class Factory(
        private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
        private val permissionController: PermissionController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(getInstalledAppsUseCase, permissionController) as T
        }
    }
}
