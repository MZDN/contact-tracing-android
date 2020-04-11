package com.wolk.android

import android.app.Application
import android.content.Intent
import android.util.Log
import com.wolk.android.ble.BLEAdvertiser
import com.wolk.android.ble.BLEForegroundService
import com.wolk.android.ble.BLEScanner
import com.wolk.android.ct.*

class CTApplication : Application() {

    var bleAdvertiser: BLEAdvertiser? = null
    var bleScanner: BLEScanner? = null

    @ExperimentalStdlibApi
    private lateinit var repo: CTRepo

    @ExperimentalStdlibApi
    fun initRepo() {
        repo = CTRepo(this, API.create())
    }

    private fun configureAdvertising(enabled: Boolean) {
        Intent(this, BLEForegroundService::class.java).also { intent ->
            if (enabled) {
                startService(intent)
            } else {
                stopService(intent)
            }
        }
    }

    @ExperimentalStdlibApi
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