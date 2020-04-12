package com.wolk.android.ct

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "contactInfo")
class ContactInfo {
    @PrimaryKey
    /** Day-level resolution that the contact occurred.  */
    var contactDate: Int = 0

    /** Length of contact in 5 minute increments.  */
    @ColumnInfo(name = "duration")
    var duration: Int = 0
}


