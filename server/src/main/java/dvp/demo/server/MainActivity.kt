package dvp.demo.server

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import dvp.demo.ble.advertising.BLEPeripheralAdvertising
import dvp.demo.ble.utils.PERMISSION_GRANTED
import dvp.demo.ble.utils.checkPermissionGranted
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val bluetoothManager by lazy {
        this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val blePeripheral by lazy {
        BLEPeripheralAdvertising(this, bluetoothManager)
    }

    private var isAlreadyAdvertising = false

    private var sendValue = "Nothing"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sendingValue.doOnTextChanged { text, _, _, _ ->
            updateSentValue(text.toString())
        }

        switchPeripheral.setOnCheckedChangeListener { _, isChecked -> setPeripheralSwitch(isChecked) }
    }

    private fun setPeripheralSwitch(isChecked: Boolean) {
        when (isChecked) {
            true -> {
                when (val resultCheckPermission = checkPermissionGranted(this)) {
                    PERMISSION_GRANTED -> {
                        updateSentValue("0000")
                    }
                    else -> {
                        Toast.makeText(this, resultCheckPermission, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            false -> {
                stopAdvertise()
                isAlreadyAdvertising = false
                sendValue = "Nothing"
            }
        }
    }

    private fun updateSentValue(text: String) {
        when {
            !switchPeripheral.isChecked -> {
                sendValue = "Nothing"
            }
            text.isBlank() || text.length < 4 -> {
                Log.d("TEST", "input error")
            }
            else -> {
                sendValue = text
                startAdvertise()
            }
        }
    }

    private fun startAdvertise() {
        stopAdvertise()
        blePeripheral.start(sendValue)
        isAlreadyAdvertising = true
    }

    private fun stopAdvertise() {
        if (isAlreadyAdvertising) {
            blePeripheral.stop()
        }
    }
}