package com.wolk.android.tcn

data class Report(var hashedPK: ByteArray, var encodedMsg: ByteArray?) {
    constructor() : this(ByteArray(0), ByteArray(0))
}

