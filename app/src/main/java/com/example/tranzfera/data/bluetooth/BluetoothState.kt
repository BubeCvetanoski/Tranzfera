package com.example.tranzfera.data.bluetooth

sealed interface BluetoothState {
    data object BluetoothConnectionSuccess : BluetoothState
    data class BluetoothConnectionError(val message: String) : BluetoothState
    data class BluetoothDataTransferSuccess(val data: BluetoothData) : BluetoothState
    data class BluetoothDataTransferError(val message: String) : BluetoothState
}