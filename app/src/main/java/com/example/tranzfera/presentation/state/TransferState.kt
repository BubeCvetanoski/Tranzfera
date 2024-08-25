package com.example.tranzfera.presentation.state

import com.example.tranzfera.data.bluetooth.model.BluetoothData

data class TransferState(
    val data: List<BluetoothData> = emptyList()
)
