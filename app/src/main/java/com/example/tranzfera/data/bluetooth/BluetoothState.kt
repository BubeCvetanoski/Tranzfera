package com.example.tranzfera.data.bluetooth

import com.example.tranzfera.data.bluetooth.model.BluetoothData

sealed interface BluetoothState {
    data object BluetoothConnectionSuccess : BluetoothState
    data class BluetoothConnectionError(val message: String) : BluetoothState
    data class BluetoothDataTransferSuccess(val data: BluetoothData) : BluetoothState
}