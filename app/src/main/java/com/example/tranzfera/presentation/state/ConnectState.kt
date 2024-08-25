package com.example.tranzfera.presentation.state

import com.example.tranzfera.data.bluetooth.model.FoundBluetoothDevice

data class ConnectState(
    val isBluetoothEnabled: Boolean = false,
    val scannedDevices: List<FoundBluetoothDevice> = emptyList(),
    val pairedDevices: List<FoundBluetoothDevice> = emptyList(),
    val connectedDevice: FoundBluetoothDevice? = null,
    val isConnectedDevice: Boolean = false,
    val errorMessage: String = ""
)
