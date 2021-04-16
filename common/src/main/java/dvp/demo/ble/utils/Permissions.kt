package dvp.demo.ble.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import dvp.demo.ble.R

const val REQ_LOCATION_CODE = 111
const val REQ_BLE_CODE = 222
const val REQ_GPS_CODE = 333


fun checkPermissionGranted(context: Context): String {
    return when {
        PermissionChecker.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PermissionChecker.PERMISSION_GRANTED -> {
            "Location"
        }
        !BluetoothAdapter.getDefaultAdapter().isEnabled -> {
            "Bluetooth"
        }
        !((context.getSystemService(Context.LOCATION_SERVICE)) as LocationManager).isProviderEnabled(
            LocationManager.GPS_PROVIDER
        ) -> {
            "GPS"
        }
        else -> PERMISSION_GRANTED
    }
}


fun Activity.checkLocationPermission(
    requestLocationLauncher: ActivityResultLauncher<String>,
    locationGranted: () -> Unit
) {
    when (PackageManager.PERMISSION_GRANTED) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) -> {
            locationGranted.invoke()
        }
        else -> {
            AlertDialog.Builder(this)
                .setTitle("Location permission")
                .setMessage("Must enable location permission")
                .setPositiveButton("OK") { _, _ ->
                    requestLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }
}


fun Activity.checkBluetoothPermission(
    requestBluetoothLauncher: ActivityResultLauncher<Intent>,
    bluetoothEnabled: () -> Unit
) {
    if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
        bluetoothEnabled.invoke()
    } else {
        AlertDialog.Builder(this)
            .setTitle("Bluetooth Permission")
            .setMessage("Must enable Bluetooth")
            .setPositiveButton("Setting") { _, _ ->
                requestBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
            .setNegativeButton("Close", null)
            .show()
    }
}


fun Activity.checkGPSPermission(
    requestGPSLauncher: ActivityResultLauncher<Intent>,
    gpsEnabled: () -> Unit
) {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        gpsEnabled.invoke()
    } else {
        AlertDialog.Builder(this)
            .setTitle("GPS Permission")
            .setMessage("Must enable GPS")
            .setPositiveButton("Setting") { _, _ ->
                requestGPSLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Close", null)
            .show()
    }
}


fun Activity.reqLocationPermission(){
    AlertDialog.Builder(this)
        .setTitle("Location Permission")
        .setMessage("Must accept location permission")
        .setPositiveButton("OK") { _, _ ->
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQ_LOCATION_CODE)
        }
        .setNegativeButton("Close", null)
        .show()
}


fun Activity.reqBluetoothEnable(){
    AlertDialog.Builder(this)
        .setTitle("Bluetooth")
        .setMessage("Must enable Bluetooth")
        .setPositiveButton("OK") { _, _ ->
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_BLE_CODE)
        }
        .setNegativeButton("Close", null)
        .show()
}


fun Activity.reqGPSEnable(){
    AlertDialog.Builder(this)
        .setTitle("GPS")
        .setMessage("Must enable GPS")
        .setPositiveButton("Setting") { _, _ ->
            startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQ_GPS_CODE)
        }
        .setNegativeButton("Close", null)
        .show()
}

