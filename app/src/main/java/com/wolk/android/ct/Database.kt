package com.wolk.android.ct

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Database(entities = [ContactInfoDAO::class,DailyTracingKeyDAO::class], version = 1, exportSchema = false)
abstract class CTDatabase : RoomDatabase() {
    abstract fun dailyTracingKeyDAO(): DailyTracingKeyDAO
    abstract fun contactInfoDAO(): ContactInfoDAO
    abstract fun rollingProximityIdentifierDAO(): RollingProximityIdentifierDAO

    companion object {
        private const val NUMBER_OF_THREADS = 4
        val databaseWriteExecutor: ExecutorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS)

        @Volatile
        private var INSTANCE: CTDatabase? = null

        fun getInstance(context: Context): CTDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                CTDatabase::class.java, "CT.db"
            ).fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
    }
}