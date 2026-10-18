package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.PermissionGroupType
import com.example.core.RiskLevel
import com.example.data.datasource.PackageManagerDataSource
import com.example.data.datasource.SpecialAccessDataSource
import com.example.data.local.PermoraDatabase
import com.example.data.repository.AppPermissionRepositoryImpl
import com.example.domain.model.AppInfo
import com.example.domain.model.AppPermission
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppPermissionRepositoryTest {

    private lateinit var database: PermoraDatabase
    private lateinit var fakePackageManagerDataSource: FakePackageManagerDataSource
    private lateinit var repository: AppPermissionRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PermoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fakePackageManagerDataSource = FakePackageManagerDataSource()
        val specialAccessDataSource = SpecialAccessDataSource(context)

        repository = AppPermissionRepositoryImpl(
            database = database,
            packageManagerDataSource = fakePackageManagerDataSource,
            specialAccessDataSource = specialAccessDataSource
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRepositoryDoesNotReuseCachedPackageInfoAndUpdatesRoomOnPermissionChange() = runTest {
        val pkg = "com.example.testapp"
        // Initial state: Camera permission granted = false
        fakePackageManagerDataSource.setPackageState(
            pkgName = pkg,
            appName = "Test App",
            permissions = listOf(
                AppPermission(
                    permission = "android.permission.CAMERA",
                    packageName = pkg,
                    group = PermissionGroupType.CAMERA,
                    isGranted = false,
                    riskLevel = RiskLevel.HIGH,
                    userFriendlyName = "Camera"
                )
            )
        )

        // Sync initial state
        repository.refreshSinglePackage(pkg)
        val initialPerms = repository.observeAppPermissions(pkg).first()
        assertEquals(1, initialPerms.size)
        assertFalse(initialPerms[0].isGranted)

        // System permission status changes in PackageManager
        fakePackageManagerDataSource.setPackageState(
            pkgName = pkg,
            appName = "Test App",
            permissions = listOf(
                AppPermission(
                    permission = "android.permission.CAMERA",
                    packageName = pkg,
                    group = PermissionGroupType.CAMERA,
                    isGranted = true,
                    riskLevel = RiskLevel.HIGH,
                    userFriendlyName = "Camera"
                )
            )
        )

        // Re-query: Repository fetches fresh data from PackageManagerDataSource and updates Room
        repository.refreshSinglePackage(pkg)
        val updatedPerms = repository.observeAppPermissions(pkg).first()
        assertEquals(1, updatedPerms.size)
        assertTrue(updatedPerms[0].isGranted)
    }

    class FakePackageManagerDataSource : PackageManagerDataSource(ApplicationProvider.getApplicationContext()) {
        private val packageMap = mutableMapOf<String, Pair<AppInfo, List<AppPermission>>>()

        fun setPackageState(pkgName: String, appName: String, permissions: List<AppPermission>) {
            val appInfo = AppInfo(
                packageName = pkgName,
                appName = appName,
                versionName = "1.0",
                isSystemApp = false,
                totalPermissionsCount = permissions.size,
                grantedPermissionsCount = permissions.count { it.isGranted }
            )
            packageMap[pkgName] = Pair(appInfo, permissions)
        }

        override suspend fun loadSinglePackage(packageName: String): Pair<AppInfo, List<AppPermission>>? {
            return packageMap[packageName]
        }

        override suspend fun loadAllInstalledApps(): List<Pair<AppInfo, List<AppPermission>>> {
            return packageMap.values.toList()
        }
    }
}
