package com.wolk.android.ct


import androidx.paging.DataSource
import androidx.room.*

@Dao
interface RollingProximityIdentifierDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(rpi: RollingProximityIdentifier)

    @Update
    fun updateKey(key: RollingProximityIdentifier)

    @Query( "DELETE FROM rollingProximityIdentifier")
    fun deleteAll()

    @get:Query("SELECT * FROM rollingProximityIdentifier ORDER BY dayNumber DESC")
    val pagedAllSortedByDescTimestamp: DataSource.Factory<Int, RollingProximityIdentifier>

    @Query("SELECT * FROM rollingProximityIdentifier where dayNumber >= :afterDayNumber")
    fun recentlySeen(afterDayNumber : UInt) : List<RollingProximityIdentifier>

    @get:Query("SELECT * FROM rollingProximityIdentifier")
    val all: List<RollingProximityIdentifier>
}

