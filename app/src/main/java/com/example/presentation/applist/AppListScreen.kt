package com.example.presentation.applist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppInfo
import com.example.domain.usecase.AppFilter
import com.example.domain.usecase.AppSort
import com.example.presentation.common.AppIconImage
import com.example.presentation.common.OnResumeEffect
import com.example.presentation.common.RiskBadge
import com.example.presentation.common.getPermissionGroupIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onNavigateToAppDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    OnResumeEffect {
        viewModel.refresh(force = false)
    }

    Scaffold(
        topBar = {
            if (uiState.isMultiSelectMode) {
                TopAppBar(
                    title = {
                        Text("${uiState.selectedPackages.size} Selected", fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier.testTag("btn_close_selection")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close selection")
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.clearSelection() }) {
                            Text("Clear")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text("Installed Apps", fontWeight = FontWeight.Bold)
                    },
                    actions = {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier.testTag("btn_sort_apps")
                        ) {
                            Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort apps")
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Highest Risk First") },
                                onClick = {
                                    viewModel.onSortChanged(AppSort.RISK_DESC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Most Permissions") },
                                onClick = {
                                    viewModel.onSortChanged(AppSort.PERMISSIONS_COUNT_DESC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A to Z)") },
                                onClick = {
                                    viewModel.onSortChanged(AppSort.NAME_ASC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z to A)") },
                                onClick = {
                                    viewModel.onSortChanged(AppSort.NAME_DESC)
                                    sortMenuExpanded = false
                                }
                            )
                        }

                        IconButton(
                            onClick = { viewModel.refresh(force = true) },
                            modifier = Modifier.testTag("btn_refresh_apps")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh apps")
                        }
                    }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search Input
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {        Text(
                        text = "Search by app name or package...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("search_apps_input")
                )

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.currentFilter == AppFilter.USER,
                        onClick = { viewModel.onFilterChanged(AppFilter.USER) },
                        label = { Text("User Apps") },
                        leadingIcon = if (uiState.currentFilter == AppFilter.USER) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("filter_user_apps")
                    )
                    FilterChip(
                        selected = uiState.currentFilter == AppFilter.RISKY,
                        onClick = { viewModel.onFilterChanged(AppFilter.RISKY) },
                        label = { Text("Risky Apps") },
                        leadingIcon = if (uiState.currentFilter == AppFilter.RISKY) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("filter_risky_apps")
                    )
                    FilterChip(
                        selected = uiState.currentFilter == AppFilter.ALL,
                        onClick = { viewModel.onFilterChanged(AppFilter.ALL) },
                        label = { Text("All Apps") },
                        leadingIcon = if (uiState.currentFilter == AppFilter.ALL) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("filter_all_apps")
                    )
                    FilterChip(
                        selected = uiState.currentFilter == AppFilter.SYSTEM,
                        onClick = { viewModel.onFilterChanged(AppFilter.SYSTEM) },
                        label = { Text("System Apps") },
                        leadingIcon = if (uiState.currentFilter == AppFilter.SYSTEM) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("filter_system_apps")
                    )
                }

                // Results count & last updated text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${uiState.displayedApps.size} apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.lastUpdatedTime > 0) {
                        Text(
                            text = "Updated: ${timeFormat.format(Date(uiState.lastUpdatedTime))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.testTag("last_updated_text")
                        )
                    }
                }

                // Apps List
                if (uiState.displayedApps.isEmpty() && !uiState.isRefreshing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No apps match current filters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("app_list_lazy_column"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.displayedApps,
                            key = { it.packageName }
                        ) { app ->
                            AppListItemRow(
                                app = app,
                                isMultiSelect = uiState.isMultiSelectMode,
                                isSelected = uiState.selectedPackages.contains(app.packageName),
                                onClick = {
                                    if (uiState.isMultiSelectMode) {
                                        viewModel.toggleAppSelection(app.packageName)
                                    } else {
                                        onNavigateToAppDetail(app.packageName)
                                    }
                                },
                                onLongClick = {
                                    if (!uiState.isMultiSelectMode) {
                                        viewModel.enterMultiSelectMode(app.packageName)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .testTag("refresh_progress_indicator")
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItemRow(
    app: AppInfo,
    isMultiSelect: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("app_row_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelect) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.testTag("checkbox_${app.packageName}")
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            AppIconImage(
                packageName = app.packageName,
                modifier = Modifier.size(46.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "System",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${app.grantedPermissionsCount}/${app.totalPermissionsCount} granted",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    app.sensitiveGroups.take(4).forEach { group ->
                        val icon = getPermissionGroupIcon(group)
                        Icon(
                            imageVector = icon,
                            contentDescription = group.displayName,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            RiskBadge(riskLevel = app.riskLevel, score = app.riskScore)
        }
    }
}
