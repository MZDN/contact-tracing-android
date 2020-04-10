package com.wolk.android.tcn

import android.app.Application
import android.os.Handler
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.room.ColumnInfo
import androidx.room.Entity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

// TCNRepo coordinates local Storage (Room) and network API calls to manage the TCN protocol
//  (A) refreshTCN: CEK generation every 15 minutes (stored in Room)
//  (B) insertTCN: Storage of observed TCNs from other BLE devices
internal class TCNRepo(application: Application, private val tcnApi: TCNApi, private val tcnUserDao: TCNUserDAO, private val tcnProximityDao: TCNProximityDAO) {

    private val tcnUserDAO: TCNUserDAO
    private val tcnProximityDAO: TCNProximityDAO
    private val tcnMatchDAO: TCNMatchDAO
    private val TAG = "Repo"

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    fun insert(tcnUser: TCNUser) {
        TCNDatabase.databaseWriteExecutor.execute { tcnUserDAO.insert(tcnUser) }
    }

    init {
        val db = TCNDatabase.getInstance(application)
        tcnUserDAO = db.tcnUserDAO()
        tcnProximityDAO = db.tcnProximityDAO()
        tcnMatchDAO = db.tcnMatchDAO()
    }

    // TCNUser
    var tcnUser: MutableLiveData<TCNUser?> = MutableLiveData<TCNUser?>()
    var memo: MutableLiveData<ByteArray> = MutableLiveData<ByteArray>()
    var latitude: MutableLiveData<Float> = MutableLiveData<Float>()
    var longitude: MutableLiveData<Float> = MutableLiveData<Float>()

    private val periodicGetReportFrequencyInSeconds = 180
    private val periodicRachetFrequencyInSeconds = 60

    // lastGetTCNReport is the last time (unix timestamp) the Report were requested
    var lastGetTCNReport = 0

    init {
        // load TCN from local storage, if it exists
        tcnUser.value = tcnUserDao.lastTCNKey()

        // Setup regular TCN refresh, which powers TCN broadcast
        ratchetTCNUser()

        // Setup period Query
        periodicQuery()
    }

    // Note: It is crucial that TCN ratchet is synchronized with MAC rotation at the Bluetooth layer to prevent linkability attacks.
    private fun ratchetTCNUser() {
        // if there is no CEK at all, generate one
        if (tcnUser.value == null) {
            tcnUser.value = TCNUser.create(memo.value, latitude.value, longitude.value)
            tcnUser.value?.let {
                tcnUserDao.insert(it)
            }
        }
        Handler().postDelayed({
            ratchetTCNUser()
        }, periodicRachetFrequencyInSeconds * 1000L)
    }

    // insertTCNProximity should be called when a peripheral TCN (not the user but other users in the BLE neighborhood) are detected
    fun insertTCNProximity(tcnProximity: TCNProximity) {
        tcnProximityDao.insert(tcnProximity)
    }

    // TCN API Calls: mapping into Server Endpoints via Retrofit
    // 1. Client posts reports to /report
    private fun postReport(reports: List<Report>) = tcnApi.postReport(reports)

    // 2. Client queries for Array<Reports> from /query/<timestamp>
    private fun query(query : ByteArray, timestamp: Int) = tcnApi.query(query, timestamp)

    // memo will be the broadcast
    fun doPostReport(memoCandidate : ByteArray) {

        // put all the data for the user in the last 2 weeks in one place
        val timeSeen = hashMapOf<Long, TCNUser>()
        var afterTS = currentTimestamp() - 86400*14
        val recentlyBroadcast = tcnUserDAO.recentlyBroadcast(afterTS)
        for (i in recentlyBroadcast.indices) {
            timeSeen[quantizeTS(recentlyBroadcast[i].timestamp)] = recentlyBroadcast[i]
        }

        val ts = currentTimestamp() - 86400 * 14 // 2 weeks
        val reports = MutableList(0) { Report() }
        tcnProximityDAO.recentlySeen(ts).map { t ->
            // publicKey of the TCNProximity
            val kf = KeyFactory.getInstance("EC")
            val receiverPublicKey: PublicKey = kf.generatePublic(X509EncodedKeySpec(t.publicKey))
            timeSeen[quantizeTS(t.timestamp)]?.let {
                // privatekey and publickey of the user
                val senderPublicKey: PublicKey = kf.generatePublic(X509EncodedKeySpec(it.publicKey))
                val senderPrivateKey: PrivateKey =
                    kf.generatePrivate(PKCS8EncodedKeySpec(it.privateKey))
                val c = AESSecurityCap()
                c.setSenderKey(senderPublicKey, senderPrivateKey)
                c.setReceiverPublicKey(receiverPublicKey)
                val encMessage = c.encrypt(memo.value!!)
                reports.add(Report(H_prefix(H_tcn(it.publicKey)), encMessage))
            }
        }
        memo.value = memoCandidate
        postReport(reports)
    }


    fun H_prefix(H_pk : ByteArray) : ByteArray {
        return H_pk.copyOfRange(0, 3) // 24 bit
    }

    // checking the mailboxes of people we have seen
    fun composeQuery(recentlySeen : List<TCNProximity>) : ByteArray {
        val prefixes = hashMapOf<ByteArray, Boolean>()
        recentlySeen.map { tcnProximity ->
            tcnProximity.let {
                prefixes[H_prefix(H_tcn(it.publicKey))] = true
            }
        }
        var query = ByteArray(0)
        for ((prefix, _) in prefixes) {
            query = query.plus(prefix)
        }
        return query
    }

    // periodicQuery fetches a Reports by checking "mailboxes" of proximity
    private fun periodicQuery() {
        if ( true ) {
            return
        }
        // for all the recently seen H(PK)
        val startTimestamp = 0L
        var recentlyBroadcast = tcnUserDao.recentlyBroadcast(startTimestamp)
        var recentlySeen = tcnProximityDao.recentlySeen(startTimestamp)
        val query = composeQuery(recentlySeen)
        val call = query(query, lastGetTCNReport)
        call.enqueue(object : Callback<Array<Report>> {
                override fun onResponse(call: Call<Array<Report>?>?, response: Response<Array<Report>>) {
                        val statusCode: Int = response.code()
                        if ( statusCode == 200 ) {
                            val r: Array<Report>? = response.body()
                            r?.let {
                                    // compute matches
                                    val matches = findMatchedReports(r, recentlyBroadcast, recentlySeen)
                                    // insert matches into the database
                                    for ( i in matches.indices ) {
                                        tcnMatchDAO.insert(matches[i])
                                    }
                            }
                            lastGetTCNReport = (System.currentTimeMillis() / 1000L).toInt()
                        } else {
                            Log.i(TAG, "periodicQuery $statusCode")
                        }
                }
                override fun onFailure(call: Call<Array<Report>?>?, t: Throwable?) {
                    // Log error here since request failed
                    Log.i(TAG, "periodicQuery Failure")
                }
        }
        )

        Handler().postDelayed({
            periodicQuery()
        }, periodicGetReportFrequencyInSeconds * 1000L)
    }

    fun decryptReport(u : TCNUser, proximityPublicKey : ByteArray, report : Report) : TCNMatch? {
        // TCNUser has public/privatekey of user at a certain time
        val privateKey = u.privateKey
        val publicKey = u.publicKey

        // report has encodedMemo
        val encodedMsg = report.encodedMsg

        // TODO: attempt a decryption
        var memo = ByteArray(0)

        return TCNMatch(proximityPublicKey, u.timestamp, u.latitude, u.longitude, memo)

    }

    fun quantizeTS(ts : Long)  : Long {
        val ts = ( ts / 60 ) * 60
        return 0
    }

    fun findMatchedReports(reports: Array<Report>, recentlyBroadcast : List<TCNUser>, recentlySeen : List<TCNProximity>) : List<TCNMatch> {
        // for all the recentlySeen, organize them by prefixes of the Hash
        val map = hashMapOf<ByteArray, MutableList<ByteArray>>()
        val timeSeen = hashMapOf<ByteArray, Long>()
        for (i in recentlySeen.indices) {
            val pk = recentlySeen[i].publicKey
            val H_pk = H_prefix(H_tcn(pk))
            if ( map[H_pk] == null ) {
                map[H_pk] = MutableList<ByteArray>(0) { _ ->ByteArray(0) }
            }
            map[H_pk]?.add(pk)
            timeSeen[pk] = quantizeTS(recentlySeen[i].timestamp)
        }

        val matches = MutableList<TCNMatch>(0) { _ -> TCNMatch() }
        for (i in reports.indices) {
            val report = reports[i]
            // KEY JOIN IS HERE:
            //   prefix of the report (report.hashedPK)
            //   prefix of the TCNProximity records (map, organized by
            map[report.hashedPK]?.let {  // it =  list of the publickeys that the user has recentlyseen
                if ( it.size > 0 ) { // most of the time this never happens, but when it does
                    for ( j in it.indices ) {
                        // pk is a candidate public key in one of the public keys that map into this bucket
                        val pk = it[j]
                        timeSeen[pk]?.let {
                            val match = decryptReport(tcnUserDAO.getTCNUser(it, it + 60), pk, report)
                            if ( match != null ) {
                                matches.add(match)
                            }
                        }

                    }
                }
            }
        }
        return matches.toList()
    }
}
