package com.example.tranzfera.data.bluetooth.model

import android.annotation.SuppressLint
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