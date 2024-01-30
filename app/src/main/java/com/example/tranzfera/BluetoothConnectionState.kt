package com.example.tranzfera

sealed interface BluetoothConnectionState {
    data object BluetoothConnectionSuccess : BluetoothConnectionState
    data class BluetoothConnectionError(val message: String) : BluetoothConnectionState
}