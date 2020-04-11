package com.wolk.android.ct


import androidx.paging.DataSource
import androidx.room.*
import java.util.*

@Dao
interface ContactInfoDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(contactInfo: ContactInfo)

    @Update
    fun updateKey(key: ContactInfo)

    @Query( "DELETE FROM contactInfo")
    fun deleteAll()

    @get:Query("SELECT * FROM contactInfo ORDER BY contactDate DESC")
    val pagedAllSortedByDescTimestamp: DataSource.Factory<Int, ContactInfo>

    @Query("SELECT * FROM contactInfo where contactDate > :afterDate")
    fun recentlySeen(afterDate : Date) : List<ContactInfo>

    @get:Query("SELECT * FROM contactinfo")
    val all: List<ContactInfo>
}

