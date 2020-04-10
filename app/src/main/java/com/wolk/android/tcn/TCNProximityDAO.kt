package com.wolk.android.tcn

import androidx.paging.DataSource
import androidx.room.*

@Dao
interface TCNProximityDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tcn: TCNProximity)

    @Update
    fun updateKey(key: TCNProximity)

    @Query( "DELETE FROM tcnproximity")
    fun deleteAll()

    @get:Query("SELECT * FROM tcnproximity ORDER BY timestamp DESC")
    val pagedAllSortedByDescTimestamp: DataSource.Factory<Int, TCNProximity>

    @Query("SELECT * FROM tcnproximity where timestamp > :afterTS")
    fun recentlySeen(afterTS : Long) : List<TCNProximity>

    @get:Query("SELECT * FROM tcnproximity")
    val all: List<TCNProximity>
}

