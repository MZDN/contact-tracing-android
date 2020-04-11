package com.wolk.android.ct

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.PublicKey
import java.util.*
import javax.crypto.KeyAgreement
import java.io.IOException
import java.security.*
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec

@ExperimentalUnsignedTypes
fun compareByteArray(a : UByteArray, b : UByteArray) : Int {
    for (i in 0..a.size) {
        if (a[i] != b[i]) {
            return a[i].toInt() - b[i].toInt()
        }
    }
    return 0
}
fun ByteArray.toHexString() : String {
    return this.joinToString("") {
        java.lang.String.format("%02x", it)
    }
}

@ExperimentalUnsignedTypes
fun computeUUIDHash(selfUUID: UUID, otherUUID: UUID) : ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    val selfBytes = getBytesFromUUID(selfUUID).toUByteArray()
    val otherBytes = getBytesFromUUID(otherUUID).toUByteArray()
    var bytes: UByteArray
    if ( compareByteArray(selfBytes, otherBytes) < 0 ) {
        bytes = selfBytes + otherBytes
    } else {
        bytes = otherBytes + selfBytes
    }
    return md.digest(bytes.toByteArray()).toUByteArray().asByteArray()
}

fun UIntToByteArray(i: UInt): ByteArray {
    val bb: ByteBuffer = ByteBuffer.allocate(Integer.SIZE / java.lang.Byte.SIZE)
    bb.order(ByteOrder.BIG_ENDIAN)
    bb.putInt(i.toInt())
    return bb.array()
}

fun IntToByteArray(i: Int): ByteArray {
    val bb: ByteBuffer = ByteBuffer.allocate(Integer.SIZE / java.lang.Byte.SIZE)
    bb.order(ByteOrder.BIG_ENDIAN)
    bb.putInt(i)
    return bb.array()
}

fun byteArrayToInt(b: ByteArray): Int {
    val bb: ByteBuffer = ByteBuffer.wrap(b)
    bb.order(ByteOrder.BIG_ENDIAN)
    return bb.getInt()
}


fun getBytesFromUUID(uuid: UUID): ByteArray {
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.putLong(uuid.mostSignificantBits)
    bb.putLong(uuid.leastSignificantBits)
    return bb.array()
}

fun getUUIDFromBytes(bytes: ByteArray): UUID {
    val byteBuffer = ByteBuffer.wrap(bytes)
    val high = byteBuffer.long
    val low = byteBuffer.long
    return UUID(high, low)
}

fun getDateStamp(timeStamp : Long) : String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val date = Date( timeStamp * 1000L)
    return  sdf.format(date)
}

fun ByteArray.toHex() = this.joinToString(separator = "") { it.toInt().and(0xff).toString(16).padStart(2, '0') }

class CommonHash {
    constructor()

    constructor(hexs : String) {
        val s = hexs.toCharArray()
        val len = s.size
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
    }

    var bytes = ByteArray(32)

    override fun toString(): String {
        var out = ""
        for (j in bytes.indices) {
            out += String.format("%02x", bytes[j])
        }
        return out

    }
}



fun dayNumber(ts : Long) : UInt {
    return (ts / 86400).toUInt()
}

fun timeIntervalNumber(ts : Long) : UInt {
    return (ts % 600).toUInt()
}

fun currentTimestamp() : Long {
    return (System.currentTimeMillis() / 1000L)
}

