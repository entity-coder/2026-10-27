package com.repairdesk.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Single Room database for the app. Version 1 ships jobs + payments;
 * destructive migration is acceptable for a v1 shop app with no released
 * data to preserve, and it keeps the setup honest instead of pretending
 * a migration path exists.
 */
@Database(entities = [Job::class, Payment::class], version = 1, exportSchema = false)
abstract class RepairDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao

    companion object {
        @Volatile
        private var INSTANCE: RepairDatabase? = null

        fun get(context: Context): RepairDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RepairDatabase::class.java,
                    "repairdesk.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
