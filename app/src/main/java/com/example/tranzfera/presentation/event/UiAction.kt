package com.example.tranzfera.presentation.event

import com.example.tranzfera.data.bluetooth.BluetoothData
import com.example.tranzfera.data.bluetooth.DataType
import com.example.tranzfera.data.bluetooth.FoundBluetoothDevice

sealed class UiAction {
    data object OnBluetoothEnableClick : UiAction()
    data object ResetTransferedData : UiAction()
    data object StartBluetoothServer : UiAction()
    data class OnButtonScanClick(val onChangeButtonText: (String) -> Unit) : UiAction()
    data class OnButtonStopScanClick(val onChangeButtonText: (String) -> Unit) : UiAction()
    data class OnPairedDeviceClick(val foundBluetoothDevice: FoundBluetoothDevice) : UiAction()
    data class OnScannedDeviceClick(val foundBluetoothDevice: FoundBluetoothDevice) : UiAction()
    data class OnSendData(val data: BluetoothData, val dataType: DataType) : UiAction()
}