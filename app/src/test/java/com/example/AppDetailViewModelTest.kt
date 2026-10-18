package com.example

import com.example.core.PermissionGroupType
import com.example.core.Result
import com.example.core.RiskLevel
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
import com.example.presentation.appdetail.AppDetailUiEvent
import com.example.presentation.appdetail.AppDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testPkg = "com.sample.target"

    private lateinit var fakeRepository: FakeAppPermissionRepository
    private lateinit var fakeController: FakePermissionController
    private lateinit var viewModel: AppDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAppPermissionRepository(testPkg)
        fakeController = FakePermissionController()

        val getAppPermissionsUseCase = GetAppPermissionsUseCase(fakeRepository)
        val setPermissionStateUseCase = SetPermissionStateUseCase(fakeController, fakeRepository)
        val revokeAllPermissionsUseCase = RevokeAllPermissionsUseCase(fakeController, fakeRepository)
        val undoRevokeUseCase = UndoRevokeUseCase(fakeController, fakeRepository)

        viewModel = AppDetailViewModel(
            packageName = testPkg,
            getAppPermissionsUseCase = getAppPermissionsUseCase,
            setPermissionStateUseCase = setPermissionStateUseCase,
            revokeAllPermissionsUseCase = revokeAllPermissionsUseCase,
            undoRevokeUseCase = undoRevokeUseCase,
            permissionController = fakeController
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testViewModelRefreshesOnResumeAndEmitsNewStateWhenSystemStateChanges() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val initialUiState = viewModel.uiState.value
        assertEquals(testPkg, initialUiState.appInfo?.packageName)
        assertFalse(initialUiState.permissions[0].isGranted)

        // System state changes externally
        fakeRepository.updatePermissionState("android.permission.CAMERA", true)

        // Simulate ON_RESUME refresh call
        viewModel.refresh(force = true)
        advanceUntilIdle()

        val updatedUiState = viewModel.uiState.value
        assertTrue(updatedUiState.permissions[0].isGranted)
    }

    @Test
    fun testTogglePermissionReportsVerificationFailureWhenStateNotApplied() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        var receivedEvent: AppDetailUiEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { receivedEvent = it }
        }
        advanceUntilIdle()

        // Grant command succeeds in exit code, but fake repository maintains state as false
        fakeController.setReturnCommandSuccess(true)

        viewModel.togglePermission("android.permission.CAMERA", true)
        advanceUntilIdle()

        assertTrue(receivedEvent is AppDetailUiEvent.ShowSnackbar)
        assertEquals("Permission change was not applied", (receivedEvent as AppDetailUiEvent.ShowSnackbar).message)
    }

    class FakeAppPermissionRepository(private val testPkg: String) : AppPermissionRepository {
        override var pendingPackageName: String? = null

        private val appFlow = MutableStateFlow<AppInfo?>(
            AppInfo(packageName = testPkg, appName = "Sample Target", versionName = "1.0", isSystemApp = false)
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

        override fun observeInstalledApps(): Flow<List<AppInfo>> = TODO("Not needed for this test")
        override fun observeAppDetails(packageName: String): Flow<AppInfo?> = appFlow
        override fun observeAppPermissions(packageName: String): Flow<List<AppPermission>> = permsFlow
        override fun observeAppsByPermissionGroup(group: PermissionGroupType): Flow<List<AppInfo>> = TODO("Not needed")

        override suspend fun refreshInstalledApps(force: Boolean, targetedPackage: String?): Result<List<AppInfo>> {
            return Result.Success(listOfNotNull(appFlow.value))
        }

        override suspend fun refreshSinglePackage(packageName: String): Result<Pair<AppInfo, List<AppPermission>>?> {
            val app = appFlow.value ?: return Result.Success(null)
            val perms = permsFlow.value
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
        private var returnCommandSuccess = true

        fun setReturnCommandSuccess(success: Boolean) {
            returnCommandSuccess = success
        }

        override fun getModeStatus(): ModeStatus {
            return ModeStatus(ControllerMode.SHIZUKU_PRIVILEGED, true, true, "Active")
        }

        override fun isPrivilegedMode(): Boolean = true
        override fun requestShizukuPermission() {}

        override suspend fun grantPermission(packageName: String, permission: String): Result<Boolean> {
            return if (returnCommandSuccess) Result.Success(true) else Result.Error(RuntimeException("Failed"))
        }

        override suspend fun revokePermission(packageName: String, permission: String): Result<Boolean> {
            return if (returnCommandSuccess) Result.Success(true) else Result.Error(RuntimeException("Failed"))
        }

        override suspend fun revokeAllPermissions(packageName: String, permissions: List<String>): Result<Int> {
            return Result.Success(permissions.size)
        }

        override fun openAppPermissionSettings(packageName: String) {}
        override fun openAppDetailsSettings(packageName: String) {}
        override fun openSpecialAccessSettings(action: String) {}
    }
}
