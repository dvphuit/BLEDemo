package dvp.demo.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.os.ParcelUuid
import android.util.Log
import dvp.demo.ble.utils.TAG
import dvp.demo.ble.utils.UUID_DATA
import dvp.demo.ble.utils.UUID_SERVICE

class BLECentral(adapter: BluetoothAdapter) {

    private val bleScanner: BluetoothLeScanner = adapter.bluetoothLeScanner
    private lateinit var scanCallback: ScanCallback
    private var scanResult: ((BluetoothDevice) -> Unit)? = null
    private var value = ""
    fun startScan(value: String, result: (BluetoothDevice) -> Unit) {
        this.value = value
        this.scanResult = result

        Log.d(TAG, "Start Scan")
        val filters: MutableList<ScanFilter> = mutableListOf(
            ScanFilter.Builder().apply {
                setServiceUuid(ParcelUuid(UUID_SERVICE))
                setServiceData(ParcelUuid(UUID_DATA), value.toByteArray())
            }.build()
        )
        val settings: ScanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0)
            .build()

        scanCallback = createScanCallback()
        bleScanner.startScan(filters, settings, scanCallback)
    }

    fun stopScan() {
        Log.d(TAG, "Stop Scan")
        bleScanner.flushPendingScanResults(scanCallback)
        bleScanner.stopScan(scanCallback)
    }


    private fun createScanCallback(): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
//                Log.e("TEST", "result ${result.device}")
                scanResult?.invoke(result.device)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.e("TEST", "scan failed $errorCode")
//                startScan(value, scanResult!!)
            }
        }
    }
}
