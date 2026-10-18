package com.example.presentation.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AppInfo
import com.example.domain.usecase.AppFilter
import com.example.domain.usecase.AppSort
import com.example.domain.usecase.GetInstalledAppsUseCase
import com.example.domain.usecase.RevokeAllPermissionsUseCase
import com.example.domain.usecase.SearchFilterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val currentFilter: AppFilter = AppFilter.USER,
    val currentSort: AppSort = AppSort.RISK_DESC,
    val displayedApps: List<AppInfo> = emptyList(),
    val totalCount: Int = 0,
    val selectedPackages: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val isBulkRevoking: Boolean = false,
    val bulkRevokeResult: String? = null,
    val lastUpdatedTime: Long = 0L
)

class AppListViewModel(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val searchFilterUseCase: SearchFilterUseCase,
    private val revokeAllPermissionsUseCase: RevokeAllPermissionsUseCase
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val currentFilter = MutableStateFlow(AppFilter.USER)
    private val currentSort = MutableStateFlow(AppSort.RISK_DESC)
    private val selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val isMultiSelect = MutableStateFlow(false)
    private val isRevoking = MutableStateFlow(false)
    private val messageFlow = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<AppListUiState> = combine(
        getInstalledAppsUseCase(),
        searchQuery,
        currentFilter,
        currentSort,
        selectedPackages,
        isMultiSelect,
        isRevoking,
        messageFlow,
        isRefreshing
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val apps = params[0] as List<AppInfo>
        val query = params[1] as String
        val filter = params[2] as AppFilter
        val sort = params[3] as AppSort
        val selected = params[4] as Set<String>
        val multiSelect = params[5] as Boolean
        val revoking = params[6] as Boolean
        val msg = params[7] as String?
        val refreshing = params[8] as Boolean

        val filtered = searchFilterUseCase(apps, query, filter, sort)
        val lastUpdated = apps.maxOfOrNull { it.lastCheckedAt } ?: 0L

        AppListUiState(
            isLoading = apps.isEmpty() && refreshing,
            isRefreshing = refreshing,
            searchQuery = query,
            currentFilter = filter,
            currentSort = sort,
            displayedApps = filtered,
            totalCount = apps.size,
            selectedPackages = selected,
            isMultiSelectMode = multiSelect,
            isBulkRevoking = revoking,
            bulkRevokeResult = msg,
            lastUpdatedTime = lastUpdated
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppListUiState(isLoading = true)
    )

    init {
        refresh(force = false)
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onFilterChanged(filter: AppFilter) {
        currentFilter.value = filter
    }

    fun onSortChanged(sort: AppSort) {
        currentSort.value = sort
    }

    fun toggleAppSelection(packageName: String) {
        val current = selectedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        selectedPackages.value = current
        if (current.isEmpty()) {
            isMultiSelect.value = false
        } else {
            isMultiSelect.value = true
        }
    }

    fun enterMultiSelectMode(initialPackage: String) {
        isMultiSelect.value = true
        selectedPackages.value = setOf(initialPackage)
    }

    fun clearSelection() {
        isMultiSelect.value = false
        selectedPackages.value = emptySet()
    }

    fun dismissMessage() {
        messageFlow.value = null
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            if (force) {
                isRefreshing.value = true
            }
            getInstalledAppsUseCase.refresh(force = force)
            isRefreshing.value = false
        }
    }

    class Factory(
        private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
        private val searchFilterUseCase: SearchFilterUseCase,
        private val revokeAllPermissionsUseCase: RevokeAllPermissionsUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppListViewModel(
                getInstalledAppsUseCase,
                searchFilterUseCase,
                revokeAllPermissionsUseCase
            ) as T
        }
    }
}
