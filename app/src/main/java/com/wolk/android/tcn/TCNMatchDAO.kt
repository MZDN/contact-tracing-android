package com.wolk.android.tcn


import androidx.paging.DataSource
import androidx.room.*

@Dao
interface TCNMatchDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tcn: TCNMatch)

    @Update
    fun updateKey(key: TCNMatch)

    @Query( "DELETE FROM tcnmatch")
    fun deleteAll()

    @get:Query("SELECT * FROM tcnmatch ORDER BY timestamp DESC")
    val pagedAllSortedByDescTimestamp: DataSource.Factory<Int, TCNMatch>

    @Query("SELECT * FROM tcnmatch where timestamp > :afterTS")
    fun recentlySeen(afterTS : Long) : List<TCNMatch>

    @get:Query("SELECT * FROM tcnmatch")
    val all: List<TCNMatch>
}

