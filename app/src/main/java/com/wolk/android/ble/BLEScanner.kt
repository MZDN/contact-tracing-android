package com.wolk.android.ble

import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import com.wolk.android.R
import com.wolk.android.utils.toUUID
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit
import com.wolk.android.ble.BluetoothService.TCN_CHARACTERISTIC
import com.wolk.android.ble.BluetoothService.TCN_SERVICE
import java.util.*

class BLEScanner(val context: Context, adapter: BluetoothAdapter) {

    companion object {
        private const val TAG = "BluetoothLeScanner"
    }

    private val scanner: BluetoothLeScanner? = adapter.bluetoothLeScanner
    private var mGatt : BluetoothGatt? = null

    var isScanning: Boolean = false
    private var handler = Handler()

    private class GattClientCallback : BluetoothGattCallback() {
        //var mConnected = false
        //var mInitialized = false

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {

            Log.i(TAG,"[onConnectionStateChange] status: $status and newState: $newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG,"[onConnectionStateChange] Connected to GATT server.")
                    Log.i(TAG,"[onConnectionStateChange] Attempting to start service discovery: " + gatt.discoverServices())
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG,"[onConnectionStateChange] Disconnected from GATT server.")
                    //broadcastUpdate(intentAction)
                    if (status == 133) {
                        val myContext: Context? = null
                        gatt.device.connectGatt(myContext, false, this, TRANSPORT_LE)
                    }
                }
            }
        }

        // New services discovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.i(TAG,"onServicesDiscovered called: looking for - uuid: " + BluetoothService.TCN_SERVICE)
            val discoveredService : BluetoothGattService?
            var discoveredChar : BluetoothGattCharacteristic
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    discoveredService = gatt.getService(BluetoothService.TCN_SERVICE)
                    Log.i(TAG,"[onServicesDiscovered] discoveredServices $discoveredService")
                    discoveredChar = discoveredService.getCharacteristic(BluetoothService.TCN_CHARACTERISTIC)
                    Log.i(TAG,"[onServicesDiscovered] discoveredChar $discoveredChar has a value of: " + discoveredChar.getValue())
                    gatt.readCharacteristic(discoveredChar)
                    /* TODO: Review -- for iOS, we "readValue" but this code is trying to write
                        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        mInitialized = gatt.setCharacteristicNotification(characteristic, true);
                        // send a message back  https://www.bignerdranch.com/blog/bluetooth-low-energy-on-android-part-2/ https://github.com/bignerdranch/android-bluetooth-testbed/tree/a/android-ble-part-2
                        val message = "1234567890"
                        Log.i(TAG, "onServicesDiscovered setCharacteristicNotification $mInitialized-$message")
                        val messageBytes = message.toByteArray()
                        characteristic.setValue(messageBytes)

                        val success = gatt.writeCharacteristic(characteristic)
                        Log.i(TAG, "onServicesDiscovered writeCharacteristic $success")
                        return
                     */
                    return
                }
                else -> {
                    Log.i(TAG, "[onServicesDiscovered] NOT SUCCESS - received status: $status")
                    return
                }
            }
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
            Log.i(TAG,"[onReliableWriteCompleted] called")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.i(TAG,"[onCharacteristicWrite] called")
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.i(TAG,"[onCharacteristicRead] called w/ status = $status and Chara ${characteristic.value}")
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    Log.i(TAG,"[onCharacteristicRead] characterisitic is $characteristic")
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt:BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.i(TAG,"[onCharacteristicChanged] called")
        }
    }

    private var scanCallback = object : ScanCallback() {

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.i(TAG, "onScanFailed errorCode=$errorCode")
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.i(TAG, "onBatchScanResults results=$results")
            results?.forEach { addScanResult(it) }
        }

        private fun addScanResult(result: ScanResult) {
            val device = result.device
            val deviceAddress = device.address
            mScanResults.put(deviceAddress, device)
            //delay 1s
            connectDevice(device)

            val scanRecord = result.scanRecord ?: return
            val tcn = scanRecord.serviceData[ParcelUuid(BluetoothService.TCN_SERVICE)]?.toUUID()
            if (tcn == null) {
                Log.i(TAG,"Scan result device.address=${result.device.address} RSSI=${result.rssi} TCN=N/A")
            } else {
                Log.i(TAG,"Scan result device.address=${result.device.address} RSSI=${result.rssi} TCN=${tcn.toString().toUpperCase()}")
//                TCNDatabase.databaseWriteExecutor.execute {
//                    val dao: TCNProximityDAO = TCNDatabase.getInstance(context).tcnProximityDAO()
//                    val temporaryContact = TCNProximity.observe(publicKey, sigToVerify, memo)
//                }
            }
        }

        private fun connectDevice(device: BluetoothDevice) {
            val gattClientCallback = GattClientCallback()
            mGatt = device.connectGatt(context, false, gattClientCallback, TRANSPORT_LE)
            //mGatt = device.connectGatt(context, true, gattClientCallback)
        }
    }

    val mScanResults = HashMap<String, BluetoothDevice>();

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun startScanning(serviceUUIDs: Array<UUID>?) {
        if (isScanning) return

        try {
            val scanFilters = serviceUUIDs?.map {
                ScanFilter.Builder().setServiceUuid(ParcelUuid(it)).build()
            }

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(1000)
                .build()

            isScanning = true
            scanner?.startScan(scanFilters, scanSettings, scanCallback)
            Log.i(TAG, "Started scan")
        } catch (exception: Exception) {
            Log.e(TAG, "Start scan failed: $exception")
        }

        // Bug workaround: Restart periodically so the Bluetooth daemon won't get into a borked state
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (isScanning) {
                Log.i(TAG, "Restarting scan...")
                stopScanning()
                startScanning(serviceUUIDs)
            }
        }, TimeUnit.SECONDS.toMillis(100))
    }

    private fun scanComplete() {
        if (mScanResults.isEmpty()) {
            return
        }
        for (deviceAddress in mScanResults) {
            Log.d(TAG, "Found device: $deviceAddress")
        }
    }

    fun stopScanning() {
        if (!isScanning) return

        try {
            isScanning = false
            scanner?.stopScan(scanCallback)
            scanComplete();
            Log.i(TAG, "Stopped scan")
        } catch (exception: Exception) {
            Log.e(TAG, "Stop scan failed: $exception")
        }
    }
}