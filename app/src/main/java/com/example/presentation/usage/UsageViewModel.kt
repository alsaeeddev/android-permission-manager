package com.example.presentation.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.PermissionGroupType
import com.example.domain.model.PermissionUsageRecord
import com.example.domain.usecase.GetPermissionUsageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UsageUiState(
    val isLoading: Boolean = true,
    val selectedFilter: PermissionGroupType? = null,
    val logs: List<PermissionUsageRecord> = emptyList()
)

class UsageViewModel(
    private val getPermissionUsageUseCase: GetPermissionUsageUseCase
) : ViewModel() {

    private val allLogs = MutableStateFlow<List<PermissionUsageRecord>>(emptyList())
    private val selectedFilter = MutableStateFlow<PermissionGroupType?>(null)
    private val isLoading = MutableStateFlow(true)

    val uiState: StateFlow<UsageUiState> = combine(
        allLogs,
        selectedFilter,
        isLoading
    ) { logs, filter, loading ->
        val filtered = if (filter == null) logs else logs.filter { it.group == filter }
        UsageUiState(
            isLoading = loading,
            selectedFilter = filter,
            logs = filtered
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UsageUiState(isLoading = true)
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isLoading.value = true
            allLogs.value = getPermissionUsageUseCase()
            isLoading.value = false
        }
    }

    fun setFilter(group: PermissionGroupType?) {
        selectedFilter.value = group
    }

    class Factory(
        private val getPermissionUsageUseCase: GetPermissionUsageUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UsageViewModel(getPermissionUsageUseCase) as T
        }
    }
}
