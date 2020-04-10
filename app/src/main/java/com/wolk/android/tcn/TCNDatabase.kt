package com.wolk.android.tcn

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Database(entities = [TCNUser::class,TCNProximity::class,TCNMatch::class], version = 2, exportSchema = false)
abstract class TCNDatabase : RoomDatabase() {
    abstract fun tcnUserDAO(): TCNUserDAO
    abstract fun tcnProximityDAO(): TCNProximityDAO
    abstract fun tcnMatchDAO(): TCNMatchDAO

    companion object {
        private const val NUMBER_OF_THREADS = 4
        val databaseWriteExecutor: ExecutorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS)

        @Volatile
        private var INSTANCE: TCNDatabase? = null

        fun getInstance(context: Context): TCNDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                TCNDatabase::class.java, "tcn.db"
            ).fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
    }
}