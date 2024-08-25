package com.example.tranzfera.presentation.event

import com.example.tranzfera.data.bluetooth.model.BluetoothData
import com.example.tranzfera.data.bluetooth.model.DataType
import com.example.tranzfera.data.bluetooth.model.FoundBluetoothDevice

sealed class UiAction {
    data object OnBluetoothEnableClick : UiAction()
    data object ResetTransferredData : UiAction()
    data object StartBluetoothServer : UiAction()
    data object OnDisconnectIconClick : UiAction()
    data object OnDropPairedDevicesClick : UiAction()
    data class OnButtonScanClick(val onChangeButtonText: (String) -> Unit) : UiAction()
    data class OnButtonStopScanClick(val onChangeButtonText: (String) -> Unit) : UiAction()
    data class OnPairedDeviceClick(val foundBluetoothDevice: FoundBluetoothDevice) : UiAction()
    data class OnScannedDeviceClick(val foundBluetoothDevice: FoundBluetoothDevice) : UiAction()
    data class OnSendData(val data: BluetoothData, val dataType: DataType) : UiAction()
    data class OnSaveImageClick(val imageSaverAction: suspend () -> Unit) : UiAction()
}