package com.example.di

import android.content.Context
import com.example.core.CoroutineDispatchers
import com.example.core.DefaultCoroutineDispatchers
import com.example.data.controller.CompositePermissionController
import com.example.data.datasource.PackageManagerDataSource
import com.example.data.datasource.SpecialAccessDataSource
import com.example.data.local.PermoraDatabase
import com.example.data.repository.AppPermissionRepositoryImpl
import com.example.data.repository.SettingsRepositoryImpl
import com.example.domain.repository.AppPermissionRepository
import com.example.domain.repository.PermissionController
import com.example.domain.repository.SettingsRepository
import com.example.domain.usecase.ExportReportUseCase
import com.example.domain.usecase.GetAppPermissionsUseCase
import com.example.domain.usecase.GetAppsByPermissionUseCase
import com.example.domain.usecase.GetInstalledAppsUseCase
import com.example.domain.usecase.GetPermissionUsageUseCase
import com.example.domain.usecase.RevokeAllPermissionsUseCase
import com.example.domain.usecase.SearchFilterUseCase
import com.example.domain.usecase.SetPermissionStateUseCase
import com.example.domain.usecase.UndoRevokeUseCase

interface AppContainer {
    val dispatchers: CoroutineDispatchers
    val permissionController: PermissionController
    val appPermissionRepository: AppPermissionRepository
    val settingsRepository: SettingsRepository

    val getInstalledAppsUseCase: GetInstalledAppsUseCase
    val getAppPermissionsUseCase: GetAppPermissionsUseCase
    val setPermissionStateUseCase: SetPermissionStateUseCase
    val revokeAllPermissionsUseCase: RevokeAllPermissionsUseCase
    val undoRevokeUseCase: UndoRevokeUseCase
    val getPermissionUsageUseCase: GetPermissionUsageUseCase
    val getAppsByPermissionUseCase: GetAppsByPermissionUseCase
    val searchFilterUseCase: SearchFilterUseCase
    val exportReportUseCase: ExportReportUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val dispatchers: CoroutineDispatchers by lazy {
        DefaultCoroutineDispatchers()
    }

    private val database: PermoraDatabase by lazy {
        PermoraDatabase.getInstance(context)
    }

    private val packageManagerDataSource: PackageManagerDataSource by lazy {
        PackageManagerDataSource(context)
    }

    private val specialAccessDataSource: SpecialAccessDataSource by lazy {
        SpecialAccessDataSource(context)
    }

    override val permissionController: PermissionController by lazy {
        CompositePermissionController(context, repositoryProvider = { appPermissionRepository })
    }

    override val appPermissionRepository: AppPermissionRepository by lazy {
        AppPermissionRepositoryImpl(
            database = database,
            packageManagerDataSource = packageManagerDataSource,
            specialAccessDataSource = specialAccessDataSource
        )
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(context)
    }

    override val getInstalledAppsUseCase: GetInstalledAppsUseCase by lazy {
        GetInstalledAppsUseCase(appPermissionRepository)
    }

    override val getAppPermissionsUseCase: GetAppPermissionsUseCase by lazy {
        GetAppPermissionsUseCase(appPermissionRepository)
    }

    override val setPermissionStateUseCase: SetPermissionStateUseCase by lazy {
        SetPermissionStateUseCase(permissionController, appPermissionRepository)
    }

    override val revokeAllPermissionsUseCase: RevokeAllPermissionsUseCase by lazy {
        RevokeAllPermissionsUseCase(permissionController, appPermissionRepository)
    }

    override val undoRevokeUseCase: UndoRevokeUseCase by lazy {
        UndoRevokeUseCase(permissionController, appPermissionRepository)
    }

    override val getPermissionUsageUseCase: GetPermissionUsageUseCase by lazy {
        GetPermissionUsageUseCase(appPermissionRepository)
    }

    override val getAppsByPermissionUseCase: GetAppsByPermissionUseCase by lazy {
        GetAppsByPermissionUseCase(appPermissionRepository)
    }

    override val searchFilterUseCase: SearchFilterUseCase by lazy {
        SearchFilterUseCase()
    }

    override val exportReportUseCase: ExportReportUseCase by lazy {
        ExportReportUseCase()
    }
}
