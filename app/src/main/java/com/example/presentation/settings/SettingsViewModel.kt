package com.example.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AppInfo
import com.example.domain.model.AppPermission
import com.example.domain.model.ModeStatus
import com.example.domain.repository.AppPermissionRepository
import com.example.domain.repository.PermissionController
import com.example.domain.repository.SettingsRepository
import com.example.domain.usecase.ExportReportUseCase
import com.example.domain.usecase.GetInstalledAppsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

data class SettingsUiState(
    val themeMode: String = "SYSTEM",
    val scanFrequencyHours: Int = 24,
    val modeStatus: ModeStatus,
    val showGuideDialog: Boolean = false,
    val showPrivacyPolicyDialog: Boolean = false,
    val isExporting: Boolean = false
)

sealed interface SettingsUiEvent {
    data class ShowSnackbar(val message: String) : SettingsUiEvent
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val permissionController: PermissionController,
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val appPermissionRepository: AppPermissionRepository,
    private val exportReportUseCase: ExportReportUseCase
) : ViewModel() {

    private val modeStatus = MutableStateFlow(permissionController.getModeStatus())
    private val guideDialogVisible = MutableStateFlow(false)
    private val privacyDialogVisible = MutableStateFlow(false)
    private val isExportingState = MutableStateFlow(false)

    private val _events = MutableSharedFlow<SettingsUiEvent>()
    val events: SharedFlow<SettingsUiEvent> = _events.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeMode,
        settingsRepository.scanFrequencyHours,
        modeStatus,
        guideDialogVisible,
        privacyDialogVisible,
        isExportingState
    ) { params ->
        SettingsUiState(
            themeMode = params[0] as String,
            scanFrequencyHours = params[1] as Int,
            modeStatus = params[2] as ModeStatus,
            showGuideDialog = params[3] as Boolean,
            showPrivacyPolicyDialog = params[4] as Boolean,
            isExporting = params[5] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(modeStatus = permissionController.getModeStatus())
    )

    fun setTheme(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setScanFrequency(hours: Int) {
        viewModelScope.launch {
            settingsRepository.setScanFrequencyHours(hours)
        }
    }

    fun refreshModeStatus() {
        modeStatus.value = permissionController.getModeStatus()
    }

    fun requestShizukuPermission() {
        permissionController.requestShizukuPermission()
        refreshModeStatus()
    }

    fun setGuideDialogVisible(visible: Boolean) {
        guideDialogVisible.value = visible
    }

    fun setPrivacyDialogVisible(visible: Boolean) {
        privacyDialogVisible.value = visible
    }

    fun exportReport(context: Context, uri: Uri, isJson: Boolean) {
        viewModelScope.launch {
            isExportingState.value = true
            try {
                val appPairs = withContext(Dispatchers.IO) {
                    val apps = mutableListOf<AppInfo>()
                    val permMap = mutableMapOf<String, List<AppPermission>>()
                    val entities = appPermissionRepository.refreshInstalledApps()
                    // Get all
                    val list = when (entities) {
                        is com.example.core.Result.Success -> entities.data
                        else -> emptyList()
                    }
                    for (app in list) {
                        apps.add(app)
                        permMap[app.packageName] = appPermissionRepository.getAppPermissions(app.packageName)
                    }
                    Pair(apps, permMap)
                }

                val content = if (isJson) {
                    exportReportUseCase.generateJson(appPairs.first, appPairs.second)
                } else {
                    exportReportUseCase.generateCsv(appPairs.first, appPairs.second)
                }

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(content)
                        }
                    }
                }
                _events.emit(SettingsUiEvent.ShowSnackbar("Report successfully exported!"))
            } catch (e: Exception) {
                _events.emit(SettingsUiEvent.ShowSnackbar("Export failed: ${e.message}"))
            } finally {
                isExportingState.value = false
            }
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val permissionController: PermissionController,
        private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
        private val appPermissionRepository: AppPermissionRepository,
        private val exportReportUseCase: ExportReportUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                settingsRepository,
                permissionController,
                getInstalledAppsUseCase,
                appPermissionRepository,
                exportReportUseCase
            ) as T
        }
    }
}
