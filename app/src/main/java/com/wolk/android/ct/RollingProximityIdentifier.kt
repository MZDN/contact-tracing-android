package com.wolk.android.ct


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "rollingProximityIdentifier")
data class RollingProximityIdentifier(
    @PrimaryKey
    var rpi: ByteArray? = null,

    @ColumnInfo(name = "dayNumber")
    var dayNumber: UInt = 0u,

    @ColumnInfo(name = "timeIntervalNumber")
    var timeIntervalNumber: UInt = 0u,

    @ColumnInfo(name = "duration")
    var duration: UInt = 0u
)