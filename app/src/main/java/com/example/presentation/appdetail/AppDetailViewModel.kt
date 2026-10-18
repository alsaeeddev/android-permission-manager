package com.example.presentation.appdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.Result
import com.example.domain.model.AppInfo
import com.example.domain.model.AppPermission
import com.example.domain.model.ModeStatus
import com.example.domain.model.SpecialAccessItem
import com.example.domain.repository.PermissionController
import com.example.domain.usecase.GetAppPermissionsUseCase
import com.example.domain.usecase.RevokeAllPermissionsUseCase
import com.example.domain.usecase.SetPermissionStateUseCase
import com.example.domain.usecase.UndoRevokeUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val appInfo: AppInfo? = null,
    val permissions: List<AppPermission> = emptyList(),
    val specialAccessItems: List<SpecialAccessItem> = emptyList(),
    val modeStatus: ModeStatus,
    val isToggling: Boolean = false,
    val showRevokeAllDialog: Boolean = false,
    val lastUpdatedTime: Long = 0L
)

sealed interface AppDetailUiEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : AppDetailUiEvent
}

class AppDetailViewModel(
    private val packageName: String,
    private val getAppPermissionsUseCase: GetAppPermissionsUseCase,
    private val setPermissionStateUseCase: SetPermissionStateUseCase,
    private val revokeAllPermissionsUseCase: RevokeAllPermissionsUseCase,
    private val undoRevokeUseCase: UndoRevokeUseCase,
    private val permissionController: PermissionController
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val isToggling = MutableStateFlow(false)
    private val showRevokeAllDialog = MutableStateFlow(false)
    private val specialAccessState = MutableStateFlow<List<SpecialAccessItem>>(emptyList())

    private val _events = MutableSharedFlow<AppDetailUiEvent>()
    val events: SharedFlow<AppDetailUiEvent> = _events.asSharedFlow()

    val uiState: StateFlow<AppDetailUiState> = combine(
        getAppPermissionsUseCase.observeApp(packageName),
        getAppPermissionsUseCase.observePermissions(packageName),
        specialAccessState,
        isRefreshing,
        isToggling,
        showRevokeAllDialog
    ) { params ->
        val app = params[0] as AppInfo?
        @Suppress("UNCHECKED_CAST")
        val perms = params[1] as List<AppPermission>
        @Suppress("UNCHECKED_CAST")
        val specials = params[2] as List<SpecialAccessItem>
        val refreshing = params[3] as Boolean
        val toggling = params[4] as Boolean
        val showDialog = params[5] as Boolean

        val lastUpdated = app?.lastCheckedAt ?: perms.maxOfOrNull { it.lastCheckedAt } ?: 0L

        AppDetailUiState(
            isLoading = app == null && refreshing,
            isRefreshing = refreshing,
            appInfo = app,
            permissions = perms,
            specialAccessItems = specials,
            modeStatus = permissionController.getModeStatus(),
            isToggling = toggling,
            showRevokeAllDialog = showDialog,
            lastUpdatedTime = lastUpdated
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppDetailUiState(
            isLoading = true,
            modeStatus = permissionController.getModeStatus()
        )
    )

    init {
        refresh(force = true)
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            isRefreshing.value = true
            getAppPermissionsUseCase.refreshApp(packageName)
            val specials = getAppPermissionsUseCase.getSpecialAccess(packageName)
            specialAccessState.value = specials
            isRefreshing.value = false
        }
    }

    fun togglePermission(permission: String, enable: Boolean) {
        viewModelScope.launch {
            isToggling.value = true
            val result = setPermissionStateUseCase(packageName, permission, enable)
            when (result) {
                is Result.Success -> {
                    if (result.data) {
                        if (permissionController.isPrivilegedMode()) {
                            _events.emit(AppDetailUiEvent.ShowSnackbar(
                                if (enable) "Permission granted" else "Permission revoked"
                            ))
                        } else {
                            _events.emit(AppDetailUiEvent.ShowSnackbar("Opened system settings for $packageName"))
                        }
                    } else {
                        _events.emit(AppDetailUiEvent.ShowSnackbar("Permission change was not applied"))
                    }
                }
                is Result.Error -> {
                    _events.emit(AppDetailUiEvent.ShowSnackbar(result.message ?: "Permission change was not applied"))
                }
                Result.Loading -> {}
            }
            refresh(force = true)
            isToggling.value = false
        }
    }

    fun showRevokeAllConfirmation(show: Boolean) {
        showRevokeAllDialog.value = show
    }

    fun revokeAllPermissions() {
        viewModelScope.launch {
            showRevokeAllConfirmation(false)
            val grantedPerms = uiState.value.permissions.filter { it.isGranted }.map { it.permission }
            if (grantedPerms.isEmpty()) {
                _events.emit(AppDetailUiEvent.ShowSnackbar("No granted permissions to revoke"))
                return@launch
            }

            val result = revokeAllPermissionsUseCase(packageName, grantedPerms)
            when (result) {
                is Result.Success -> {
                    _events.emit(AppDetailUiEvent.ShowSnackbar("Revoked permissions", "UNDO"))
                }
                is Result.Error -> {
                    _events.emit(AppDetailUiEvent.ShowSnackbar("Error: ${result.message}"))
                }
                Result.Loading -> {}
            }
            refresh(force = true)
        }
    }

    fun undoRevoke() {
        viewModelScope.launch {
            val result = undoRevokeUseCase(packageName)
            when (result) {
                is Result.Success -> {
                    _events.emit(AppDetailUiEvent.ShowSnackbar("Restored ${result.data} permissions"))
                }
                is Result.Error -> {
                    _events.emit(AppDetailUiEvent.ShowSnackbar("Undo failed: ${result.message}"))
                }
                Result.Loading -> {}
            }
            refresh(force = true)
        }
    }

    fun openAppDetailsSettings() {
        permissionController.openAppDetailsSettings(packageName)
    }

    fun openPermissionSettings() {
        permissionController.openAppPermissionSettings(packageName)
    }

    class Factory(
        private val packageName: String,
        private val getAppPermissionsUseCase: GetAppPermissionsUseCase,
        private val setPermissionStateUseCase: SetPermissionStateUseCase,
        private val revokeAllPermissionsUseCase: RevokeAllPermissionsUseCase,
        private val undoRevokeUseCase: UndoRevokeUseCase,
        private val permissionController: PermissionController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppDetailViewModel(
                packageName,
                getAppPermissionsUseCase,
                setPermissionStateUseCase,
                revokeAllPermissionsUseCase,
                undoRevokeUseCase,
                permissionController
            ) as T
        }
    }
}
