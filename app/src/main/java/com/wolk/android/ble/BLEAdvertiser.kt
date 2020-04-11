package com.wolk.android.ble

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wolk.android.ct.*
import java.util.*

class BLEAdvertiser(val context: Context, adapter: BluetoothAdapter) {

    companion object {
        private const val TAG = "BluetoothLeAdvertiser"
    }

    private val adapterIsEnabled: Boolean = adapter.isEnabled
    private val adapterIsMultipleAdvertisementSupported: Boolean = adapter.isMultipleAdvertisementSupported

    private val advertiser: BluetoothLeAdvertiser? = adapter.bluetoothLeAdvertiser

    private var bluetoothGattServer: BluetoothGattServer? = null

    private val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.w(TAG, "onStartSuccess settingsInEffect=$settingsInEffect")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "onStartFailure errorCode=$errorCode")
        }
    }

    private var bluetoothGattServerCallback: BluetoothGattServerCallback =
        object : BluetoothGattServerCallback() {

            override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
                super.onServiceAdded(status, service)
                Log.i(TAG, "onServiceAdded status=$status service=$service")
            }

            override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
                Log.i(TAG, "[onCharacteristicReadRequest] requestID=$requestId offset=$offset")
                var result = BluetoothGatt.GATT_SUCCESS
                var value: ByteArray? = null
                try {
                    if (characteristic?.uuid == BluetoothService.TCN_CHARACTERISTIC) {
                        if (offset != 0) {
                            result = BluetoothGatt.GATT_INVALID_OFFSET
                            return
                        }

                        //TODO: log this? modify this to store bytearray?
                        //logContactEventIdentifier(newContactEventIdentifier)
                        value = randomByteArray(512)
                        Log.i(TAG, "BLEAdvertiser [onCharacteristicReadRequest] $value")
                    } else {
                        result = BluetoothGatt.GATT_FAILURE
                    }
                } catch (exception: Exception) {
                    result = BluetoothGatt.GATT_FAILURE
                    value = null
                } finally {
                    Log.i(TAG,"onCharacteristicReadRequest result=$result device=$device requestId=$requestId offset=$offset characteristic=$characteristic")
                    bluetoothGattServer?.sendResponse(device, requestId, result, offset, value)
                }
            }

            override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?,
                preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)

                Log.i(TAG, "onCharacteristicWriteRequest requestID=$requestId offset=$offset value=$value of size: ${value?.size}")

                var result = BluetoothGatt.GATT_SUCCESS
                try {
                    if (characteristic?.uuid == BluetoothService.TCN_CHARACTERISTIC) {
                        if (offset != 0) {
                            result = BluetoothGatt.GATT_INVALID_OFFSET
                            return
                        }
                        Log.i(TAG, "[onCharacteristicWriteRequest] matched characteristic")
                    } else {
                        result = BluetoothGatt.GATT_FAILURE
                    }
                } catch (exception: Exception) {
                    result = BluetoothGatt.GATT_FAILURE
                } finally {
                    Log.i(TAG, "onCharacteristicWriteRequest value=$value | result=$result device=$device requestId=$requestId characteristic=$characteristic preparedWrite=$preparedWrite responseNeeded=$responseNeeded offset=$offset value=$value")
                    if (responseNeeded) {
                        bluetoothGattServer?.sendResponse(device, requestId, result, offset,null)
                    }
                }
            }
        }

    fun randomByteArray( length: Int ) : ByteArray {
        var bucket:String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        var randomString: String = ""
        for (i in 0..length) {
            randomString = randomString + bucket.random()
        }
        return randomString.toByteArray()
    }

    fun startAdvertising(serviceUUID: UUID?) {
        try {
            val advertiseSettings = AdvertiseSettings.Builder()
                    // Use low latency mode so the chance of being discovered is higher
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    // Use low power so the discoverability range is short
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                    // Use true so devices can connect to our GATT server
                .setConnectable(true)
                    // Advertise forever
                .setTimeout(0)
                .build()

            val advertiseData = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(serviceUUID))
                .build()

            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager).let { bluetoothManager ->

                bluetoothGattServer =
                    bluetoothManager?.openGattServer(context, bluetoothGattServerCallback)

                val service = BluetoothGattService(BluetoothService.TCN_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)
                val initialCharacteristic = BluetoothGattCharacteristic(
                    BluetoothService.TCN_CHARACTERISTIC,
                    BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
                )

                Log.i(TAG, "ADDING characteristic $initialCharacteristic | value: ${initialCharacteristic.value}")
                service.addCharacteristic(
                    initialCharacteristic
                )

                bluetoothGattServer?.clearServices()
                Log.i(TAG, "ADDING Service $service")
                bluetoothGattServer?.addService(service)
            }

            advertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)

            Log.i(TAG, "Started advertising $advertiseData")

        } catch (exception: java.lang.Exception) {
            Log.e(TAG, "Start advertising failed: $exception")
        }
    }

    fun stopAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            bluetoothGattServer?.apply {
                clearServices()
                close()
            }
            bluetoothGattServer = null

            Log.i(TAG, "Stopped advertising")
        } catch (exception: java.lang.Exception) {
            Log.e(TAG, "Stop advertising failed: $exception")
        }
    }

    // Changes the TCN to a new random UUID in the service data field
    // NOTE: This will also log the TCN and stop/start the advertiser
    fun changeContactEventIdentifierInServiceDataField() {
        Log.i(TAG, "Changing the contact event identifier in service data field...")
        stopAdvertising()
        //REVIEW: this needed? logContactEventIdentifier
        //val newContactEventIdentifier = ByteArray(16)
        //logContactEventIdentifier(newContactEventIdentifier)
        startAdvertising(BluetoothService.TCN_SERVICE)
    }

    fun logRollingIdentifier(rpi: ByteArray) {
        CTDatabase.databaseWriteExecutor.execute {
            val dao: RollingProximityIdentifierDAO = CTDatabase.getInstance(context).rollingProximityIdentifierDAO()
            val ts = currentTimestamp()
            val r = RollingProximityIdentifier(rpi, dayNumber(ts), timeIntervalNumber(ts), 1)
            dao.insert(r)
        }
    }


}