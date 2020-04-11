package com.wolk.android.ct

import android.app.PendingIntent
import android.content.Intent
import android.app.Application
import android.os.Handler
import android.util.Log
import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.random.Random.Default.nextBytes
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.mac.MacFactory
import com.google.crypto.tink.mac.MacKeyTemplates
import com.google.crypto.tink.subtle.Hkdf.computeHkdf


// CTRepo coordinates local Storage (Room) and network API calls
// to manage the Apple/Google Contact Tracing protocol with Room
// interacting with a Wolk Contact Tracing via the CTAPI
@ExperimentalStdlibApi
internal class CTRepo(application: Application, private val ctAPI: API) : ContactTracing {

    private val contactInfoDAO: ContactInfoDAO
    private val dailyTracingKeyDAO: DailyTracingKeyDAO
    private val rollingProximityIdentifierDAO: RollingProximityIdentifierDAO
    private val TAG = "CT"

    private val periodicGetDiagnosisFrequencyInSeconds = 180
    private val periodicRachetFrequencyInSeconds = 600

    // the 32-byte Tracing Key (never leaves the device)
    private var tracingKey : ByteArray = ByteArray(32)
    var dailyTracingKey: MutableLiveData<DailyTracingKey?> = MutableLiveData<DailyTracingKey?>()

    init {
        val db = CTDatabase.getInstance(application)
        dailyTracingKeyDAO = db.dailyTracingKeyDAO()
        contactInfoDAO = db.contactInfoDAO()
        rollingProximityIdentifierDAO = db.rollingProximityIdentifierDAO()
    }

    // TracingKey is generated when contact tracing is enabled on the device and is securely stored on the device
    fun generateTracingKey() {
        // The 32 byte Tracing key is derived as follows
        // TODO: put it in the keystore
        tracingKey = nextBytes(tracingKey, 0, 32)
    }

    // A Daily Tracing Key is generated for every 24-hour window where the protocol is advertising
    // From the Tracing key, we drive the 16-byte Daily Tracing Key in the following way:
    //   dtk <- HKDF(tk, NULL, (UTF("CT-DTK")||D_i), 16)
    @ExperimentalStdlibApi
    fun generateDailyTracingKey() {
        val s = "CT-DTK".encodeToByteArray()
        val dayNum = dayNumber(currentTimestamp())
        val info = s.plus(UIntToByteArray(dayNum))
        dailyTracingKey.value =  DailyTracingKey ( HKDF(tracingKey, null, info, 16), dayNum )
    }

    // HKDF designates the HKDF function as defined by RFC 5869, using the SHA-256 hash function
    fun HKDF(ikm : ByteArray, salt : ByteArray?, info : ByteArray, outputLength : Int) : ByteArray {
        // https://google.github.io/tink/javadoc/tink-android/1.0.0/index.html?com/google/crypto/tink/subtle/Hkdf.html
        return computeHkdf("HMACSHA256", ikm, salt, info, outputLength)
    }

    fun HMAC(key : ByteArray, data : ByteArray) : ByteArray {
        // TODO: set key using ByteArray
        val keysetHandle = KeysetHandle.generateNew(MacKeyTemplates.HMAC_SHA256_128BITTAG)
        val mac = MacFactory.getPrimitive(keysetHandle)
        return mac.computeMac(data)
    }


    fun insert(dtk: DailyTracingKey) {
        CTDatabase.databaseWriteExecutor.execute { dailyTracingKeyDAO.insert(dtk) }
    }

    // lastGetDiagnosisKey is the last time (unix timestamp) the Report were requested
    var lastGetDiagnosisKey = 0

    init {
        // TODO: generate this only once, put in key storage
        generateTracingKey()

        // Setup regular Rolling Proximity Identify refresh, which powers BLE broadcast
        ratchetRollingProximityIdentifier()

        // Setup regular query for
        periodicGetDiagnosisKeys()
    }

    private fun computeRollingProximityIdentifier() {

    }

    @ExperimentalStdlibApi
    private fun ratchetRollingProximityIdentifier() {
        val s = "CT-RPI".encodeToByteArray()
        val info = s.plus(UIntToByteArray(timeIntervalNumber(currentTimestamp())))
        dailyTracingKey.value?.let {
            val rpi = HMAC(it.key, info)
        }
        Handler().postDelayed({
            ratchetRollingProximityIdentifier()
        }, periodicRachetFrequencyInSeconds * 1000L)
    }

    // insertProximity should be called when a peripheral TCN (not the user but other users in the BLE neighborhood) are detected
    fun insertRPI(rpi: RollingProximityIdentifier) {
        rollingProximityIdentifierDAO.insert(rpi)
    }

    // Contact Tracing API Calls
    // 1. Client posts reports to /report
    private fun postDiagnosisKeys(diagnosisKeys: List<DailyTracingKey>) = ctAPI.postDiagnosisKeys(diagnosisKeys)

    // 2. Client queries for Array<Reports> from /diagnoses/<startDayNumber>
    private fun getDiagnosisKeys(dayNumber: UInt) = ctAPI.getDiagnosisKeys(dayNumber)

    fun doPostDiagnosisKeys(dayNumber : UInt) {

        // put all the data for the user in the last 2 weeks in one place
        val timeSeen = hashMapOf<UInt, DailyTracingKey>()
        var afterDay = dayNumber(currentTimestamp()) - 14u
        val recentDailyTracingKey = dailyTracingKeyDAO.recentDailyTracingKey(afterDay)
        for (i in recentDailyTracingKey.indices) {
            val dayNumber = recentDailyTracingKey[i].dayNumber
            timeSeen[dayNumber] = recentDailyTracingKey[i]
        }

        val ts = currentTimestamp() - 86400 * 14 // 2 weeks
        val diagnosisKeys = mutableListOf<DailyTracingKey>()
        rollingProximityIdentifierDAO.recentlySeen(dayNumber).map { t ->
            // TODO
        }
    }

    private fun periodicGetDiagnosisKeys() {
        // for all the recently seen H(PK)
        val startDay = dayNumber(currentTimestamp()) - 14u
        var recentlyBroadcast = dailyTracingKeyDAO.recentDailyTracingKey(startDay)
        var recentlySeen = rollingProximityIdentifierDAO.recentlySeen(startDay)
        val call = getDiagnosisKeys(startDay)
        call.enqueue(object : Callback<List<DailyTracingKey>> {
                override fun onResponse(call: Call<List<DailyTracingKey>?>?, response: Response<List<DailyTracingKey>>) {
                        val statusCode: Int = response.code()
                        if ( statusCode == 200 ) {
                            val r: List<DailyTracingKey>? = response.body()
                            r?.let {
                                    // compute matches
                                    val matches = findMatchedReports(recentlyBroadcast, recentlySeen)
                                    // insert matches into the database
                                    for ( i in matches.indices ) {
                                        contactInfoDAO.insert(matches[i])
                                    }
                            }
                            lastGetDiagnosisKey = (System.currentTimeMillis() / 1000L).toInt()
                        } else {
                            Log.i(TAG, "periodicGetDiagnosisKeys $statusCode")
                        }
                }
                override fun onFailure(call: Call<List<DailyTracingKey>?>?, t: Throwable?) {
                    // Log error here since request failed
                    Log.i(TAG, "periodicGetDiagnosisKeys Failure")
                }
        }
        )

        Handler().postDelayed({
            periodicGetDiagnosisKeys()
        }, periodicGetDiagnosisFrequencyInSeconds * 1000L)
    }


    fun findMatchedReports(recentlyBroadcast : List<DailyTracingKey>, recentlySeen : List<RollingProximityIdentifier>) : List<ContactInfo> {
        // for all the recentlySeen, organize them by prefixes of the Hash
        val map = hashMapOf<Int, MutableList<ByteArray>>()
        val timeSeen = hashMapOf<ByteArray, Long>()
        for (i in recentlySeen.indices) {
            // TODO
        }

        val matches = MutableList<ContactInfo>(0) { _ -> ContactInfo() }
        for (i in recentlyBroadcast.indices) {
            val dailyTracingKey = recentlyBroadcast[i]
            // KEY JOIN IS HERE:
            //   dayNumber of the DailyTracingKey
            //   dayNumber of the RollingProximityIdentifier
            map[dailyTracingKey.dayNumber]?.let {
                    for ( j in it.indices ) {
                        val pk = it[j]
                        timeSeen[pk]?.let {
                            val match = ContactInfo()
                            matches.add(match)
                        }
                    }
            }
        }
        return matches.toList()
    }

    /**
     * Flags daily tracing keys as to be stored on the server.
     *
     * This should only be done after proper verification is performed on the
     * client side that the user is diagnosed positive.
     *
     * Calling this will invoke the
     * ContactTracingCallback.requestUploadDailyTracingKeys callback
     * provided via startContactTracing at some point in the future. Provided keys
     * should be uploaded to the server and distributed to other users.
     *
     * This shows a user dialog for sharing and uploading data to the server.
     * The status will also flip back off again after 14 days; in other words,
     * the client will stop receiving requestUploadDailyTracingKeys
     * callbacks after that time.
     *
     * Only 14 days of history are available.
     */
    override fun startSharingDailyTracingKeys() : Status {
        return  Status.SUCCESS // TODO
    }


    /**
     * Provides a list of diagnosis keys for contact checking. The keys are to be
     * provided by a centralized service (e.g. synced from the server).
     *
     * When invoked after the requestProvideDiagnosisKeys callback, this triggers a
     * recalculation of contact status which can be obtained via hasContact()
     * after the calculation has finished.
     *
     * Should be called with a maximum of N keys at a time.
     */
    override fun provideDiagnosisKeys(keys: List<DailyTracingKey?>?): Status? {
        return   Status.SUCCESS // TODO
    }

    /**
     * Handles an intent which was invoked via the contactTracingCallback and
     * calls the corresponding ContactTracingCallback methods.
     */
    override fun handleIntent(intentCallback: Intent?, callback: ContactTracingCallback?) {

    }

    /**
     * Starts BLE broadcasts and scanning based on the defined protocol.
     *
     * If not previously used, this shows a user dialog for consent to start contact
     * tracing and get permission.
     *
     * Calls back when data is to be pushed or pulled from the client, see
     * ContactTracingCallback.
     *
     * Callers need to re-invoke this after each device restart, providing a new
     * callback PendingIntent.
     */
    override fun startContactTracing(contactTracingCallback: PendingIntent?): Status? {
        return  Status.SUCCESS // TODO
    }

    /**
     * Disables advertising and scanning related to contact tracing. Contents of the
     * database and keys will remain.
     *
     * If the client app has been uninstalled by the user, this will be automatically
     * invoked and the database and keys will be wiped from the device.
     */
    override fun stopContactTracing(): Status? {
        return  Status.SUCCESS // TODO
    }

    // Indicates whether contact tracing is currently running for the requesting app.
    override fun isContactTracingEnabled(): Status? {
        return  Status.SUCCESS // TODO
    }

    // getMaxDiagnosisKeys - The maximum number of keys to pass into provideDiagnosisKeys at any given time.
    override fun getMaxDiagnosisKeys(): Int {
        return 100000
    }

    /**
     * Check if this user has come into contact with a provided key. Contact
     * calculation happens daily.
     */
    override fun hasContact(k : DailyTracingKey): Boolean? {
        return   false // TODO
    }

    /**
     * Check if this user has come into contact with a provided key. Contact
     * calculation happens daily.
     */
    override fun getContactInformation(k : DailyTracingKey): List<ContactInfo?>? {
        val l = MutableList<ContactInfo?>(0) { _ -> ContactInfo() }
        return l
    }
}
