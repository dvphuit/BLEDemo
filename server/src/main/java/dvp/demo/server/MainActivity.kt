package dvp.demo.server

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import dvp.demo.ble.BLEPeripheral
import dvp.demo.ble.utils.checkPermissionGranted
import dvp.demo.ble.utils.reqBluetoothEnable
import dvp.demo.ble.utils.reqGPSEnable
import dvp.demo.ble.utils.reqLocationPermission
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), IPresenter {
    private val bluetoothManager by lazy {
        this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val blePeripheral by lazy {
        BLEPeripheral(this, bluetoothManager)
    }

    private var isAlreadyStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        switchPeripheral.setOnCheckedChangeListener { _, isChecked ->

            if (!isPassPermissionAndSetting()) {
                switchPeripheral.isChecked = false
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                startAdvertising("BLE", 10)
            } else {
                stopAdvertising()
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


    override fun startAdvertising(value: String, freqPerSec: Int) {
        startBlePeripheral(value)
        observeWrite()
    }

    override fun onReceivedValue(value: String) {
        when (value) {
            "RED" -> {
                ledView.setBackgroundColor(getColor(R.color.colorRed))
            }
            "GREEN" -> {
                ledView.setBackgroundColor(getColor(R.color.colorGreen))
            }
            else -> {
                ledView.setBackgroundColor(getColor(R.color.colorGrey))
            }
        }
    }

    override fun stopAdvertising() {
        if (isAlreadyStarted) {
            blePeripheral.stop()
        }
    }

    private fun startBlePeripheral(value: String) {
        if (isAlreadyStarted) {
            blePeripheral.stop()
        }
        blePeripheral.start(value)
        isAlreadyStarted = true
    }

    private fun observeWrite() {
        GlobalScope.launch {
            blePeripheral.getWriteResponseFlow()
                .conflate()
                .collect {
                    onReceivedValue(it)
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAdvertising()
    }
}
