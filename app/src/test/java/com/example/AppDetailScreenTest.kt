package com.example

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import com.example.core.PermissionGroupType
import com.example.core.Result
import com.example.domain.model.AppInfo
import com.example.domain.model.AppPermission
import com.example.domain.model.ControllerMode
import com.example.domain.model.ModeStatus
import com.example.domain.model.PermissionUsageRecord
import com.example.domain.model.SpecialAccessItem
import com.example.domain.model.UndoSnapshot
import com.example.domain.repository.AppPermissionRepository
import com.example.domain.repository.PermissionController
import com.example.domain.usecase.GetAppPermissionsUseCase
import com.example.domain.usecase.RevokeAllPermissionsUseCase
import com.example.domain.usecase.SetPermissionStateUseCase
import com.example.domain.usecase.UndoRevokeUseCase
import com.example.presentation.appdetail.AppDetailScreen
import com.example.presentation.appdetail.AppDetailViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testPkg = "com.sample.screen.test"

    @Test
    fun testAppDetailScreenResumesAndUpdatesPermissionSwitchState() {
        val fakeRepo = FakeAppPermissionRepository(testPkg)
        val fakeController = FakePermissionController()

        val viewModel = AppDetailViewModel(
            packageName = testPkg,
            getAppPermissionsUseCase = GetAppPermissionsUseCase(fakeRepo),
            setPermissionStateUseCase = SetPermissionStateUseCase(fakeController, fakeRepo),
            revokeAllPermissionsUseCase = RevokeAllPermissionsUseCase(fakeController, fakeRepo),
            undoRevokeUseCase = UndoRevokeUseCase(fakeController, fakeRepo),
            permissionController = fakeController
        )

        composeTestRule.setContent {
            AppDetailScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onNavigateToSpecialAccess = {}
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            viewModel.uiState.value.appInfo != null && viewModel.uiState.value.permissions.isNotEmpty()
        }

        // Scroll to switch node inside LazyColumn
        composeTestRule.onNodeWithTag("app_detail_lazy_column")
            .performScrollToNode(hasTestTag("switch_android.permission.CAMERA"))

        // Initially Camera switch is OFF
        composeTestRule.onNodeWithTag("switch_android.permission.CAMERA")
            .assertIsOff()

        // System updates permission status externally
        fakeRepo.updatePermissionState("android.permission.CAMERA", true)

        // Trigger refresh (simulating ON_RESUME lifecycle event)
        viewModel.refresh(force = true)

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            viewModel.uiState.value.permissions.firstOrNull { it.permission == "android.permission.CAMERA" }?.isGranted == true
        }

        composeTestRule.onNodeWithTag("app_detail_lazy_column")
            .performScrollToNode(hasTestTag("switch_android.permission.CAMERA"))

        // Verify UI switch recomposes and is now ON
        composeTestRule.onNodeWithTag("switch_android.permission.CAMERA")
            .assertIsOn()
    }

    class FakeAppPermissionRepository(private val testPkg: String) : AppPermissionRepository {
        override var pendingPackageName: String? = null

        private val appFlow = MutableStateFlow<AppInfo?>(
            AppInfo(packageName = testPkg, appName = "Screen Test App", versionName = "1.0", isSystemApp = false)
        )
        private val permsFlow = MutableStateFlow<List<AppPermission>>(
            listOf(
                AppPermission(
                    permission = "android.permission.CAMERA",
                    packageName = testPkg,
                    group = PermissionGroupType.CAMERA,
                    isGranted = false,
                    userFriendlyName = "Camera"
                )
            )
        )

        fun updatePermissionState(perm: String, granted: Boolean) {
            permsFlow.value = permsFlow.value.map {
                if (it.permission == perm) it.copy(isGranted = granted) else it
            }
        }

        override fun observeInstalledApps(): Flow<List<AppInfo>> = TODO("Not needed")
        override fun observeAppDetails(packageName: String): Flow<AppInfo?> = appFlow
        override fun observeAppPermissions(packageName: String): Flow<List<AppPermission>> = permsFlow
        override fun observeAppsByPermissionGroup(group: PermissionGroupType): Flow<List<AppInfo>> = TODO("Not needed")

        override suspend fun refreshInstalledApps(force: Boolean, targetedPackage: String?): Result<List<AppInfo>> {
            return Result.Success(listOfNotNull(appFlow.value))
        }

        override suspend fun refreshSinglePackage(packageName: String): Result<Pair<AppInfo, List<AppPermission>>?> {
            val app = appFlow.value ?: return Result.Success(null)
            val perms = permsFlow.value
            permsFlow.value = perms.toList()
            return Result.Success(Pair(app, perms))
        }

        override suspend fun getAppDetails(packageName: String): AppInfo? = appFlow.value
        override suspend fun getAppPermissions(packageName: String): List<AppPermission> = permsFlow.value
        override suspend fun getAppsByPermissionGroup(group: PermissionGroupType): List<AppInfo> = emptyList()
        override suspend fun getPermissionUsageHistory(): List<PermissionUsageRecord> = emptyList()
        override suspend fun getSpecialAccessItems(packageName: String): List<SpecialAccessItem> = emptyList()
        override suspend fun saveUndoSnapshot(packageName: String, grantedPermissions: List<String>): Long = 1L
        override suspend fun getLatestUndoSnapshot(packageName: String): UndoSnapshot? = null
        override suspend fun deleteUndoSnapshot(id: Long) {}
        override suspend fun syncSinglePackage(packageName: String) {}
        override suspend fun removePackage(packageName: String) {}
    }

    class FakePermissionController : PermissionController {
        override fun getModeStatus(): ModeStatus {
            return ModeStatus(ControllerMode.SHIZUKU_PRIVILEGED, true, true, "Active")
        }

        override fun isPrivilegedMode(): Boolean = true
        override fun requestShizukuPermission() {}

        override suspend fun grantPermission(packageName: String, permission: String): Result<Boolean> {
            return Result.Success(true)
        }

        override suspend fun revokePermission(packageName: String, permission: String): Result<Boolean> {
            return Result.Success(true)
        }

        override suspend fun revokeAllPermissions(packageName: String, permissions: List<String>): Result<Int> {
            return Result.Success(permissions.size)
        }

        override fun openAppPermissionSettings(packageName: String) {}
        override fun openAppDetailsSettings(packageName: String) {}
        override fun openSpecialAccessSettings(action: String) {}
    }
}
