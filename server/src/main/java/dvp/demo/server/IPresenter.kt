package dvp.demo.server

/**
 * @author dvphu on 15,April,2021
 */

interface IPresenter {
    fun startAdvertising(value: String, freqPerSec: Int)

    fun onReceivedValue(value: String)

    fun stopAdvertising()
}