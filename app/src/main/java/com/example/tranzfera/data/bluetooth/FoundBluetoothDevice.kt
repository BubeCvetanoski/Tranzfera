package com.example.tranzfera.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice

data class FoundBluetoothDevice(
    val address: String,
    val name: String?
)
@SuppressLint("MissingPermission")
fun BluetoothDevice.toFoundBluetoothDevice(): FoundBluetoothDevice {
    return FoundBluetoothDevice(
        address = address,
        name = name ?: "Nameless"
    )
}

fun FoundBluetoothDevice.toBluetoothDevice(bluetoothAdapter: BluetoothAdapter): BluetoothDevice {
    return bluetoothAdapter.getRemoteDevice(address)
}