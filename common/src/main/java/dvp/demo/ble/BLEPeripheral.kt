package dvp.demo.ble

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import dvp.demo.ble.utils.TAG
import dvp.demo.ble.utils.UUID_DATA
import dvp.demo.ble.utils.UUID_SERVICE
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow


class BLEPeripheral(private val context: Context, private val bluetoothManager: BluetoothManager) {

    private lateinit var advValue: String
    private lateinit var bluetoothGattServer: BluetoothGattServer
    private lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser
    private val registeredDevices: MutableList<BluetoothDevice> = mutableListOf()
    private val channel = Channel<String>()

    fun start(value: String) {
        this.advValue = value
        startAdvertising()
        startServer()
    }

    fun stop() {
        stopServer()
        stopAdvertising()
    }

    private fun startAdvertising() {
        val bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser

        val settings = AdvertiseSettings.Builder().apply {
            setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //100ms
            setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            setConnectable(true)
            setTimeout(0)
        }.build()

        val data = AdvertiseData.Builder().apply {
            setIncludeDeviceName(false)
            setIncludeTxPowerLevel(false)
            addServiceUuid(ParcelUuid(UUID_SERVICE))
            addServiceData(ParcelUuid(UUID_DATA), advValue.toByteArray())
        }.build()

        bluetoothLeAdvertiser.startAdvertising(
            settings,
            data,
            advertiseCallback
        )
    }

    private fun stopAdvertising() {
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
    }

    private fun startServer() {
        bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        bluetoothGattServer.addService(createService())
    }

    private fun stopServer() {
        bluetoothGattServer.close()
    }

    private fun createService(): BluetoothGattService {
        val bluetoothGattService = BluetoothGattService(
            UUID_SERVICE,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val bluetoothGattCharacteristic = BluetoothGattCharacteristic(
            UUID_DATA,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        bluetoothGattService.addCharacteristic(bluetoothGattCharacteristic)

        return bluetoothGattService
    }

    fun getWriteResponseFlow(): Flow<String> = channel.consumeAsFlow()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(TAG, "Peripheral advertise started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.i(TAG, "Peripheral advertise failed: $errorCode")
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: $device")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: $device")
                registeredDevices.remove(device)
                channel.offer("DISCONNECTED")
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (UUID_DATA == characteristic.uuid) {
                Log.i(TAG, "Read Characteristic")
                bluetoothGattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    advValue.toByteArray(Charsets.UTF_8)
                )
            } else {
                bluetoothGattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    null
                )
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (UUID_DATA == characteristic?.uuid) {
                value?.let {
                    channel.offer(String(it))
                }
            } else {
                if (responseNeeded) {
                    bluetoothGattServer.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null
                    )
                }
            }
        }

    }
}
