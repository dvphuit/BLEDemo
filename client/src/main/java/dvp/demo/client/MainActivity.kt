package dvp.demo.client

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dvp.demo.ble.BLECentral
import dvp.demo.ble.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), IPresenter {

    private lateinit var bleCentral: BLECentral
    private lateinit var bluetoothManager: BluetoothManager

    private lateinit var bluetoothAdapter: BluetoothAdapter

    private var bluetoothGatt: BluetoothGatt? = null

    private var isScanning = false
    private var isConnected = false
    private var job: Job? = null
    private var loopCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleCentral = BLECentral(bluetoothAdapter)

        switchScan.setOnCheckedChangeListener { _, isChecked ->

            if (!isPassPermissionAndSetting()) {
                switchScan.isChecked = false
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                start()
            } else {
                stop()
            }
        }
    }

    private fun isPassPermissionAndSetting(): Boolean {
        return when (checkPermissionGranted(this)) {
            "Location" -> {
                reqLocationPermission()
                false
            }
            "GPS" -> {
                reqGPSEnable()
                false
            }
            "Bluetooth" -> {
                reqBluetoothEnable()
                false
            }
            else -> true
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateStatus(status: String) = runOnUiThread {
        tvStatus.text = tvStatus.text.toString() + status + "\n"
        scrollView.fullScroll(View.FOCUS_DOWN)
    }

    override fun start() {
        startScanFor("BLE")
        updateStatus("STARTED: $loopCount")
        loopCount++
    }

    override fun stop() {
        job?.cancel()
        bleCentral.stopScan()
        bluetoothGatt?.close()
        isConnected = false
        isScanning = false

        updateStatus("STOPPED")
    }

    override fun stopScan() {
        bleCentral.stopScan()
    }

    override fun startScanFor(value: String) {
        if (isScanning) return
        bleCentral.startScan(value) {
            bleCentral.stopScan()
            isScanning = false
            connectTo(it.address)
        }
        isScanning = true
    }

    override fun connectTo(address: String) {
        if (isConnected) return
        updateStatus("Connecting to $address")
        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
        bluetoothGatt = bluetoothDevice.connectGatt(
            this,
            false,
            bluetoothGattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
    }

    override fun send(value: String) {
        if (bluetoothGatt?.connect() == true) {
            updateStatus("Sending: $value")
            val characteristic =
                bluetoothGatt!!.getService(UUID_SERVICE).getCharacteristic(UUID_DATA)
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            characteristic.value = value.toByteArray(Charsets.UTF_8)
            bluetoothGatt!!.writeCharacteristic(characteristic)
        }
    }

    override fun disconnect() {
        GlobalScope.launch {
            if (isConnected) {
                send("disconnect")
                updateStatus("Disconnect")
                updateStatus("---------------------- delay 5s")
                bluetoothGatt?.close()
                isConnected = false
                delay(1000) //stable in my case
                start()
            }
        }

    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    gatt.discoverServices()
                    isConnected = true
                    updateStatus("GATT Connected")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    updateStatus("GATT Disconnected")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    updateStatus("GATT: Services discovered")
                    job = GlobalScope.launch {
//                        delay(1000)
                        send("RED")
                        updateStatus("Sent: RED")
                        delay(1000)
                        send("GREEN")
                        updateStatus("Sent: GREEN")
                        delay(1000)
                        disconnect()
                    }
                }
                else -> {
                    updateStatus("GATT: OTHER -> $status")
                    stop()
                    start()
                }
            }
        }
    }


}