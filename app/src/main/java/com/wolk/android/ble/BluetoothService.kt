package com.wolk.android.ble

import java.util.*

object BluetoothService {

    /// The string representation of the UUID for the primary peripheral service
    var TCN_SERVICE: UUID =
        //UUID.fromString("0000C019-0000-1000-8000-00805F9B34FB")
        UUID.fromString("BC908F39-52DB-416F-A97E-6EAC29F59CA8")

    /// The string representation of the UUID for the contact event identifier characteristic
    var TCN_CHARACTERISTIC: UUID =
        //UUID.fromString("D61F4F27-3D6B-4B04-9E46-C9D2EA617F62")
        UUID.fromString("2ac35b0b-00b5-4af2-a50e-8412bcb94285")

    const val CONTACT_EVENT_NUMBER_CHANGE_INTERVAL_MIN: Int = 15
}