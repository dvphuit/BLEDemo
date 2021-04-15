package dvp.demo.client

import android.bluetooth.BluetoothDevice

/**
 * @author dvphu on 15,April,2021
 */

interface IPresenter {

    fun start()
    fun stop()

    fun startScanFor(value: String)

    fun stopScan()

    fun connectTo(address: String)

    fun send(value: String)

    fun disconnect()
}