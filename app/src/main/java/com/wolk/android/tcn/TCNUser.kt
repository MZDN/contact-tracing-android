package com.wolk.android.tcn

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.security.*
import java.security.spec.X509EncodedKeySpec

@Entity(tableName = "tcnuser")
data class TCNUser(
    @PrimaryKey
    @ColumnInfo(name = "publicKey")
    val publicKey: ByteArray,

    @ColumnInfo(name = "timestamp")
    var timestamp: Long = 0, // time of generation, not quantized

    @ColumnInfo(name = "privateKey")
    val privateKey: ByteArray,

    @ColumnInfo(name = "memo")
    val memo: ByteArray?,

    @ColumnInfo(name = "latitude")
    val latitude : Float?,

    @ColumnInfo(name = "longitude")
    val longitude : Float?,

    @ColumnInfo(name = "signature")
    val signature : ByteArray
) {
    companion object TCNUserManager {
        // create: used to generate TCN for the user every hour
        fun create(memo : ByteArray?, latitude: Float?, longitude: Float?): TCNUser {
            val gen: KeyPairGenerator = KeyPairGenerator.getInstance("EC")
            gen.initialize(256)
            val keyPair: KeyPair = gen.generateKeyPair()
            val dsa = Signature.getInstance("EC")
            dsa.initSign(keyPair.private)
            dsa.update(memo)
            val signature = dsa.sign()
            return TCNUser(keyPair.public.encoded, currentTimestamp(), keyPair.private.encoded, memo, latitude, longitude, signature)
        }
    }
}
