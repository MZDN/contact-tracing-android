package com.wolk.android.tcn

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tcnmatch")
data class TCNMatch(
    @PrimaryKey
    @ColumnInfo(name = "publicKey")
    val publicKey: ByteArray,

    @ColumnInfo(name = "timestamp")
    var timestamp: Long = 0,  // the time of observation, not quantized

    @ColumnInfo(name = "latitude")
    val latitude: Float?,  // where the user was at the time of contact

    @ColumnInfo(name = "longitude")
    val longitude: Float?, // where the user was at the time of contact

    @ColumnInfo(name = "memo")
    val memo: ByteArray
) {
    constructor() : this(ByteArray(0), 0, null, null,  ByteArray(0))
}
