package com.example.presentation.permissioncentric

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.PermissionConstants
import com.example.core.PermissionGroupType
import com.example.core.Result
import com.example.domain.model.AppInfo
import com.example.domain.model.ModeStatus
import com.example.domain.repository.PermissionController
import com.example.domain.usecase.GetAppsByPermissionUseCase
import com.example.domain.usecase.RevokeAllPermissionsUseCase
import com.example.domain.usecase.SetPermissionStateUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionCentricUiState(
    val selectedGroup: PermissionGroupType = PermissionGroupType.CAMERA,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val apps: List<AppInfo> = emptyList(),
    val modeStatus: ModeStatus,
    val isBulkRevoking: Boolean = false,
    val showBulkRevokeDialog: Boolean = false,
    val lastUpdatedTime: Long = 0L
)

class PermissionCentricViewModel(
    initialGroup: PermissionGroupType = PermissionGroupType.CAMERA,
    private val getAppsByPermissionUseCase: GetAppsByPermissionUseCase,
    private val setPermissionStateUseCase: SetPermissionStateUseCase,
    private val revokeAllPermissionsUseCase: RevokeAllPermissionsUseCase,
    private val permissionController: PermissionController
) : ViewModel() {

    private val selectedGroupState = MutableStateFlow(initialGroup)
    private val isRefreshing = MutableStateFlow(false)
    private val isBulkRevoking = MutableStateFlow(false)
    private val showBulkRevokeDialog = MutableStateFlow(false)

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PermissionCentricUiState> = combine(
        selectedGroupState,
        selectedGroupState.flatMapLatest { group -> getAppsByPermissionUseCase.observe(group) },
        isRefreshing,
        isBulkRevoking,
        showBulkRevokeDialog
    ) { params ->
        val group = params[0] as PermissionGroupType
        @Suppress("UNCHECKED_CAST")
        val appsList = params[1] as List<AppInfo>
        val refreshing = params[2] as Boolean
        val revoking = params[3] as Boolean
        val showDialog = params[4] as Boolean

        val lastUpdated = appsList.maxOfOrNull { it.lastCheckedAt } ?: 0L

        PermissionCentricUiState(
            selectedGroup = group,
            isLoading = appsList.isEmpty() && refreshing,
            isRefreshing = refreshing,
            apps = appsList,
            modeStatus = permissionController.getModeStatus(),
            isBulkRevoking = revoking,
            showBulkRevokeDialog = showDialog,
            lastUpdatedTime = lastUpdated
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PermissionCentricUiState(
            selectedGroup = initialGroup,
            isLoading = true,
            modeStatus = permissionController.getModeStatus()
        )
    )

    init {
        refresh(force = false)
    }

    fun selectGroup(group: PermissionGroupType) {
        selectedGroupState.value = group
        refresh(force = false)
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            if (force) {
                isRefreshing.value = true
            }
            getAppsByPermissionUseCase.refreshGroup(selectedGroupState.value, force = force)
            isRefreshing.value = false
        }
    }

    fun showBulkRevokeConfirmation(show: Boolean) {
        showBulkRevokeDialog.value = show
    }

    fun bulkRevokeCurrentGroup() {
        viewModelScope.launch {
            showBulkRevokeConfirmation(false)
            isBulkRevoking.value = true

            val permsForGroup = PermissionConstants.DEFINITIONS.values
                .filter { it.group == selectedGroupState.value }
                .map { it.permission }

            var revokedCount = 0
            for (app in uiState.value.apps) {
                val res = revokeAllPermissionsUseCase(app.packageName, permsForGroup)
                if (res is Result.Success) {
                    revokedCount += res.data
                }
            }

            isBulkRevoking.value = false
            _snackbarEvent.emit("Bulk revoked from apps")
            refresh(force = true)
        }
    }

    fun openAppPermissionSettings(packageName: String) {
        permissionController.openAppPermissionSettings(packageName)
    }

    class Factory(
        private val initialGroup: PermissionGroupType = PermissionGroupType.CAMERA,
        private val getAppsByPermissionUseCase: GetAppsByPermissionUseCase,
        private val setPermissionStateUseCase: SetPermissionStateUseCase,
        private val revokeAllPermissionsUseCase: RevokeAllPermissionsUseCase,
        private val permissionController: PermissionController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PermissionCentricViewModel(
                initialGroup,
                getAppsByPermissionUseCase,
                setPermissionStateUseCase,
                revokeAllPermissionsUseCase,
                permissionController
            ) as T
        }
    }
}
