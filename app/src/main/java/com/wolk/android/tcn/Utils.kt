package com.wolk.android.tcn

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

fun currentTimestamp() : Long {
    return (System.currentTimeMillis() / 1000L)
}

fun H_tcn(bytes: ByteArray): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(bytes).toUByteArray().asByteArray().copyOfRange(0,16)
}


fun computeHash(s : String) : CommonHash {
    val h = CommonHash()
    val bytes = s.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")

    val digest = md.digest(bytes).toUByteArray().asByteArray()

    h.bytes = digest
    return h
}

class AESSecurityCap internal constructor() {
    var publickey: PublicKey? = null
        private set
    var keyAgreement: KeyAgreement? = null
    var sharedsecret: ByteArray? = null

    var ALGO = "AES/GCM/NoPadding"

    // A generates a public/private key pair
    private fun makeKeyExchangeParams() {
        var kpg: KeyPairGenerator? = null
        try {
            kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(256)
            val kp = kpg.generateKeyPair()
            publickey = kp.public
            KeyAgreement.getInstance("ECDH").let {
                it.init(kp.private)
                keyAgreement = it
            }
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        }
    }

    fun setSenderKey(pubKey: PublicKey, privKey : PrivateKey) {
        publickey = pubKey
        KeyAgreement.getInstance("ECDH").let {
            it.init(privKey)
            keyAgreement = it
        }
    }

    fun setReceiverPublicKey(publickey: PublicKey?) {
        try {
            keyAgreement!!.doPhase(publickey, true)
            sharedsecret = keyAgreement!!.generateSecret()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        }
    }

    fun encrypt(msg: ByteArray?): ByteArray? {
        try {
            val key = generateKey()
            val c = Cipher.getInstance(ALGO)
            c.init(Cipher.ENCRYPT_MODE, key) // NONCE???
            return c.doFinal(msg)
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return null
    }

    fun decrypt(encryptedData: ByteArray): ByteArray {
        try {
            val key = generateKey()
            val c = Cipher.getInstance(ALGO)
            c.init(Cipher.DECRYPT_MODE, key)
            val decValue = c.doFinal(encryptedData)
            return decValue
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return encryptedData
    }

    protected fun generateKey(): Key {
        return SecretKeySpec(sharedsecret, ALGO)
    }

    init {
        makeKeyExchangeParams()
    }
}