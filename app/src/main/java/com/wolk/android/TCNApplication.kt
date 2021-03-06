package com.wolk.android

import android.app.Application
import android.content.Intent
import android.util.Log
import com.wolk.android.TCNApplication.TCNRepo
import com.wolk.android.ble.BLEAdvertiser
import com.wolk.android.ble.BLEForegroundService
import com.wolk.android.ble.BLEScanner
import com.wolk.android.tcn.*

class TCNApplication : Application() {

    var bleAdvertiser: BLEAdvertiser? = null
    var bleScanner: BLEScanner? = null

    private lateinit var repo: TCNRepo

    fun initRepo() {
        val tcnUserDao = TCNDatabase.getInstance(this).tcnUserDAO()
        val tcnProximityDao = TCNDatabase.getInstance(this).tcnProximityDAO()
        repo = TCNRepo(this, TCNApi.create(), tcnUserDao, tcnProximityDao )
    }
    internal class TCNRepo(application: Application, private val tcnApi: TCNApi, private val tcnUserDao: TCNUserDAO, private val tcnProximityDao: TCNProximityDAO)

    private fun configureAdvertising(enabled: Boolean) {
        Intent(this, BLEForegroundService::class.java).also { intent ->
            if (enabled) {
                startService(intent)
            } else {
                stopService(intent)
            }
        }
    }

    override fun onCreate() {
        Log.i("h", "x")
        super.onCreate()
        schedulePeriodicPublicContactEventsRefresh()
        val isContactEventLoggingEnabled = true
        configureAdvertising(isContactEventLoggingEnabled)
        initRepo()
    }

    private fun schedulePeriodicPublicContactEventsRefresh() {
//        val constraints = Constraints.Builder()
//            .setRequiresCharging(false)
//            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .build()
    }

    companion object {
        private const val TAG = "SixFootApplication"
    }
}