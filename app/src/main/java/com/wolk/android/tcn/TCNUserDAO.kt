package com.wolk.android.tcn

import androidx.room.*
import androidx.paging.DataSource

@Dao
interface TCNUserDAO {
    @Query("SELECT * FROM tcnuser LIMIT 1")
    fun lastTCNKey() : TCNUser

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tcn: TCNUser)

    @Update
    fun updateKey(key: TCNUser)

    @Query( "DELETE FROM tcnuser")
    fun deleteAll()

    @get:Query("SELECT * FROM tcnuser ORDER BY timestamp DESC")
    val pagedAllSortedByDescTimestamp: DataSource.Factory<Int, TCNUser>

    @get:Query("SELECT * FROM tcnuser")
    val all: List<TCNUser>

    @Query("SELECT * FROM tcnuser where timestamp >= :startTS and timestamp < :endTS LIMIT 1")
    fun getTCNUser(startTS : Long, endTS : Long) : TCNUser

    @Query("SELECT * FROM tcnuser where timestamp > :afterTS")
    fun recentlyBroadcast(afterTS : Long) : List<TCNUser>

}


