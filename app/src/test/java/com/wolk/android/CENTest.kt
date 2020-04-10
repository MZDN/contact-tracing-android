package com.wolk.android

import com.wolk.android.tcn.AESSecurityCap
import org.junit.Test
import java.security.*

// See [testing documentation](http://d.android.com/tools/testing).
class EncryptionTest {
    @Test
    fun messageGeneration() {
        // A has received B's public key already, and needs to encrypt M to send in a Report
        // but we will make B's public key here
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(256)
        val kp = kpg.generateKeyPair()
        val BpublicKey = kp.public

        val c = AESSecurityCap()
        c.setReceiverPublicKey(BpublicKey)
        val msg = "fruitcakesandmelody"
        val m = msg.toByteArray()
        val e = c.encrypt(m)
        val d = c.decrypt(e)
        System.out.println("d2: ${String(d)}")
    }
}
