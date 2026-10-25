package com.jsemalja.trackmecalories.data

import android.content.Context
import androidx.room.Room

/**
 * The single database instance, shared by the app and the home-screen widget (two
 * instances of one Room database in a process would each keep their own caches).
 */
object Database {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "track-me-calories-db")
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
            // Only destructive on downgrade (e.g. reinstalling an older debug build).
            // Upgrades must ship a real Migration so user data is never silently wiped.
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
            .also { instance = it }
    }
}
