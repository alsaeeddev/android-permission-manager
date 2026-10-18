package com.example.presentation.specialaccess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AppInfo
import com.example.domain.model.SpecialAccessItem
import com.example.domain.repository.PermissionController
import com.example.domain.usecase.GetAppPermissionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpecialAccessUiState(
    val isLoading: Boolean = true,
    val appInfo: AppInfo? = null,
    val specialAccessItems: List<SpecialAccessItem> = emptyList()
)

class SpecialAccessViewModel(
    private val packageName: String,
    private val getAppPermissionsUseCase: GetAppPermissionsUseCase,
    private val permissionController: PermissionController
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpecialAccessUiState())
    val uiState: StateFlow<SpecialAccessUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val (app, _) = getAppPermissionsUseCase(packageName)
            val items = getAppPermissionsUseCase.getSpecialAccess(packageName)
            _uiState.value = SpecialAccessUiState(
                isLoading = false,
                appInfo = app,
                specialAccessItems = items
            )
        }
    }

    fun openSettings(action: String) {
        permissionController.openSpecialAccessSettings(action)
    }

    class Factory(
        private val packageName: String,
        private val getAppPermissionsUseCase: GetAppPermissionsUseCase,
        private val permissionController: PermissionController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SpecialAccessViewModel(packageName, getAppPermissionsUseCase, permissionController) as T
        }
    }
}
