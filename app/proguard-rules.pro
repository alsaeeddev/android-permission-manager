# Shizuku
-keep class moe.shizuku.** { *; }
-keep interface moe.shizuku.** { *; }
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep models & entities
-keep class com.example.data.local.entity.** { *; }
-keep class com.example.domain.model.** { *; }
