package com.wolk.android.ct

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "dailyTracingKey")
data class DailyTracingKey(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: ByteArray,

    @ColumnInfo(name = "date")
    var dayNumber: UInt = 0u
)