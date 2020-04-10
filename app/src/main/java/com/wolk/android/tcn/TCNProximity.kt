package com.wolk.android.tcn

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

@Entity(tableName = "tcnproximity")
data class TCNProximity(
    @PrimaryKey
    @ColumnInfo(name = "publicKey")
    val publicKey: ByteArray,

    @ColumnInfo(name = "timestamp")
    var timestamp: Long = 0,  // the time of observation, not quantized

    @ColumnInfo(name = "memo")
    val memo: ByteArray
) {
    companion object TCNProximityManager {
        // observe: when observing another TCN
        fun observe(publicKey : ByteArray, sigToVerify : ByteArray, memo : ByteArray): TCNProximity? {
            val kf = KeyFactory.getInstance("EC")
            val pubKey: PublicKey = kf.generatePublic(X509EncodedKeySpec(publicKey))

            val sig = Signature.getInstance("EC")
            sig.initVerify(pubKey)
            sig.update(memo, 0, memo.size)
            val verifies = sig.verify(sigToVerify)
            if ( verifies ) {
                val receiverPublicKey: PublicKey = kf.generatePublic(X509EncodedKeySpec(publicKey))
                return TCNProximity(publicKey, currentTimestamp(), memo)
            }
            return null
        }
    }
}