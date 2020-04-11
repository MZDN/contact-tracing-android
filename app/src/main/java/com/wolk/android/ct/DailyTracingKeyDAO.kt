package com.wolk.android.ct

import androidx.paging.DataSource
import androidx.room.*

@Dao
interface DailyTracingKeyDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(dtk: DailyTracingKey)

    @Update
    fun updateKey(key: DailyTracingKey)

    @Query( "DELETE FROM dailyTracingKey")
    fun deleteAll()

    @get:Query("SELECT * FROM dailyTracingKey ORDER BY date DESC")
    val pagedAllSortedByDescTimestamp: DataSource.Factory<Int, DailyTracingKey>

    @Query("SELECT * FROM dailyTracingKey where date > :afterTS order by date")
    fun recentDailyTracingKey(afterTS : UInt) : List<DailyTracingKey>

    @get:Query("SELECT * FROM dailyTracingKey")
    val all: List<DailyTracingKey>
}

