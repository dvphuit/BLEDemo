package dvp.demo.client

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dvp.demo.ble.advertising.BLECentralAdvertising
import dvp.demo.ble.utils.PERMISSION_GRANTED
import dvp.demo.ble.utils.checkPermissionGranted
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val bleCentral by lazy {
        BLECentralAdvertising(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switchScan.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> {
                    when (val resultCheckPermission = checkPermissionGranted(this)) {
                        PERMISSION_GRANTED -> {
                            bleCentral.startScan()
                            observeResponse()
                        }
                        else -> {
                            Toast.makeText(this, resultCheckPermission, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
                false -> {
                    bleCentral.stopScan()
                }
            }
        }
    }

    private fun observeResponse() {
        GlobalScope.launch {
            bleCentral.getResponseFlow()
                .conflate()
                .collect {
                    Log.d("TEST", "received value $it")
                }
        }
    }
}