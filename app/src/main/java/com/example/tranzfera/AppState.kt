package com.example.tranzfera

data class AppState(
    val isBluetoothEnabled: Boolean = false,
    val scannedDevices: List<FoundBluetoothDevice> = emptyList(),
    val pairedDevices: List<FoundBluetoothDevice> = emptyList(),
    val connectedDevice: FoundBluetoothDevice? = null,
    val isConnectedDevice: Boolean = false,
    val errorMessage: String = ""
)
