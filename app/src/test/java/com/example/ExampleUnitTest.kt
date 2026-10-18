package com.example

import com.example.core.PermissionConstants
import com.example.core.PermissionGroupType
import com.example.core.RiskLevel
import com.example.domain.model.AppInfo
import com.example.domain.model.AppPermission
import com.example.domain.usecase.AppFilter
import com.example.domain.usecase.AppSort
import com.example.domain.usecase.ExportReportUseCase
import com.example.domain.usecase.SearchFilterUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val searchFilterUseCase = SearchFilterUseCase()
    private val exportReportUseCase = ExportReportUseCase()

    private val sampleApps = listOf(
        AppInfo(
            packageName = "com.sample.camera",
            appName = "CamPro",
            versionName = "1.0",
            isSystemApp = false,
            totalPermissionsCount = 4,
            grantedPermissionsCount = 3,
            sensitiveGrantedCount = 2,
            riskScore = 80,
            riskLevel = RiskLevel.CRITICAL,
            sensitiveGroups = listOf(PermissionGroupType.CAMERA, PermissionGroupType.MICROPHONE)
        ),
        AppInfo(
            packageName = "com.android.calculator2",
            appName = "Calculator",
            versionName = "2.1",
            isSystemApp = true,
            totalPermissionsCount = 0,
            grantedPermissionsCount = 0,
            sensitiveGrantedCount = 0,
            riskScore = 0,
            riskLevel = RiskLevel.LOW,
            sensitiveGroups = emptyList()
        ),
        AppInfo(
            packageName = "com.chat.social",
            appName = "Chatter",
            versionName = "3.2",
            isSystemApp = false,
            totalPermissionsCount = 8,
            grantedPermissionsCount = 5,
            sensitiveGrantedCount = 4,
            riskScore = 60,
            riskLevel = RiskLevel.HIGH,
            sensitiveGroups = listOf(PermissionGroupType.CONTACTS, PermissionGroupType.LOCATION)
        )
    )

    @Test
    fun testSearchFilter_FilterUserApps() {
        val result = searchFilterUseCase(sampleApps, "", AppFilter.USER, AppSort.NAME_ASC)
        assertEquals(2, result.size)
        assertTrue(result.none { it.isSystemApp })
    }

    @Test
    fun testSearchFilter_FilterRiskyApps() {
        val result = searchFilterUseCase(sampleApps, "", AppFilter.RISKY, AppSort.RISK_DESC)
        assertEquals(2, result.size)
        assertEquals("CamPro", result[0].appName)
        assertEquals("Chatter", result[1].appName)
    }

    @Test
    fun testSearchFilter_QuerySearch() {
        val result = searchFilterUseCase(sampleApps, "calc", AppFilter.ALL, AppSort.NAME_ASC)
        assertEquals(1, result.size)
        assertEquals("Calculator", result[0].appName)
    }

    @Test
    fun testExportReport_Json() {
        val perms = mapOf(
            "com.sample.camera" to listOf(
                AppPermission(
                    permission = "android.permission.CAMERA",
                    packageName = "com.sample.camera",
                    group = PermissionGroupType.CAMERA,
                    isGranted = true,
                    riskLevel = RiskLevel.HIGH,
                    userFriendlyName = "Camera"
                )
            )
        )
        val json = exportReportUseCase.generateJson(listOf(sampleApps[0]), perms)
        assertTrue(json.contains("\"appName\": \"CamPro\""))
        assertTrue(json.contains("\"permission\": \"android.permission.CAMERA\""))
    }
}
