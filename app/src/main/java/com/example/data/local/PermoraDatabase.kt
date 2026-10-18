package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AppDao
import com.example.data.local.dao.UndoDao
import com.example.data.local.dao.UsageDao
import com.example.data.local.entity.CachedAppEntity
import com.example.data.local.entity.CachedPermissionEntity
import com.example.data.local.entity.PermissionUsageEntity
import com.example.data.local.entity.UndoSnapshotEntity

@Database(
    entities = [
        CachedAppEntity::class,
        CachedPermissionEntity::class,
        UndoSnapshotEntity::class,
        PermissionUsageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PermoraDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun undoDao(): UndoDao
    abstract fun usageDao(): UsageDao

    companion object {
        @Volatile
        private var INSTANCE: PermoraDatabase? = null

        fun getInstance(context: Context): PermoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PermoraDatabase::class.java,
                    "permora_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
