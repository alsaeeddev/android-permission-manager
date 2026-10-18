package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.CachedAppEntity
import com.example.data.local.entity.CachedPermissionEntity
import com.example.data.local.entity.PermissionUsageEntity
import com.example.data.local.entity.UndoSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM cached_apps ORDER BY appName COLLATE NOCASE ASC")
    fun getAllAppsFlow(): Flow<List<CachedAppEntity>>

    @Query("SELECT * FROM cached_apps ORDER BY appName COLLATE NOCASE ASC")
    suspend fun getAllApps(): List<CachedAppEntity>

    @Query("SELECT * FROM cached_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): CachedAppEntity?

    @Query("SELECT * FROM cached_apps WHERE packageName = :packageName LIMIT 1")
    fun getAppFlow(packageName: String): Flow<CachedAppEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<CachedAppEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: CachedAppEntity)

    @Query("DELETE FROM cached_apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    @Query("SELECT * FROM cached_permissions WHERE packageName = :packageName")
    suspend fun getPermissionsForApp(packageName: String): List<CachedPermissionEntity>

    @Query("SELECT * FROM cached_permissions WHERE packageName = :packageName")
    fun getPermissionsForAppFlow(packageName: String): Flow<List<CachedPermissionEntity>>

    @Query("SELECT * FROM cached_permissions WHERE groupName = :groupName AND isGranted = 1")
    suspend fun getGrantedPermissionsByGroup(groupName: String): List<CachedPermissionEntity>

    @Query("SELECT * FROM cached_permissions WHERE groupName = :groupName AND isGranted = 1")
    fun getGrantedPermissionsByGroupFlow(groupName: String): Flow<List<CachedPermissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermissions(permissions: List<CachedPermissionEntity>)

    @Query("DELETE FROM cached_permissions WHERE packageName = :packageName")
    suspend fun deletePermissionsForApp(packageName: String)

    @Transaction
    suspend fun replaceAppData(app: CachedAppEntity, permissions: List<CachedPermissionEntity>) {
        insertApp(app)
        deletePermissionsForApp(app.packageName)
        if (permissions.isNotEmpty()) {
            insertPermissions(permissions)
        }
    }
}

@Dao
interface UndoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: UndoSnapshotEntity): Long

    @Query("SELECT * FROM undo_snapshots WHERE packageName = :packageName ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshot(packageName: String): UndoSnapshotEntity?

    @Query("DELETE FROM undo_snapshots WHERE id = :id")
    suspend fun deleteSnapshot(id: Long)
}

@Dao
interface UsageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsageLogs(logs: List<PermissionUsageEntity>)

    @Query("SELECT * FROM permission_usage_logs ORDER BY lastAccessEpochMs DESC LIMIT :limit")
    suspend fun getRecentUsageLogs(limit: Int = 100): List<PermissionUsageEntity>
}
