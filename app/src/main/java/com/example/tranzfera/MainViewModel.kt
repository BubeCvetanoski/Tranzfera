package com.example.tranzfera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tranzfera.data.bluetooth.BluetoothHandler
import com.example.tranzfera.data.bluetooth.BluetoothState
import com.example.tranzfera.data.bluetooth.model.BluetoothData
import com.example.tranzfera.data.bluetooth.model.DataType
import com.example.tranzfera.data.bluetooth.model.FoundBluetoothDevice
import com.example.tranzfera.presentation.event.UiAction
import com.example.tranzfera.presentation.state.ConnectState
import com.example.tranzfera.presentation.state.TransferState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val bluetoothHandler: BluetoothHandler
) : ViewModel() {

    private val _connectState = MutableStateFlow(ConnectState())
    val connectState = combine(
        bluetoothHandler.scannedDevices,
        bluetoothHandler.pairedDevices,
        bluetoothHandler.connectedDevice,
        bluetoothHandler.isBluetoothEnabled,
        _connectState
    ) { scannedDevices, pairedDevices, connectedDevice, isBluetoothEnabled, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
            connectedDevice = connectedDevice,
            isBluetoothEnabled = isBluetoothEnabled
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        _connectState.value
    )

    private val _transferState = MutableStateFlow(TransferState())
    val transferState: StateFlow<TransferState> = _transferState

    fun onAction(event: UiAction) {
        when (event) {
            is UiAction.OnBluetoothEnableClick -> onBluetoothEnableClick()
            is UiAction.ResetTransferredData -> resetTransferredData()
            is UiAction.StartBluetoothServer -> startBluetoothServer()
            is UiAction.OnDisconnectIconClick -> onDisconnectIconClick()
            is UiAction.OnDropPairedDevicesClick -> updatePairedDevicesState()
            is UiAction.OnButtonScanClick -> onButtonScanClick(event.onChangeButtonText)
            is UiAction.OnButtonStopScanClick -> onButtonStopScanClick(event.onChangeButtonText)
            is UiAction.OnPairedDeviceClick -> connectToDevice(event.foundBluetoothDevice)
            is UiAction.OnScannedDeviceClick -> pairDevice(event.foundBluetoothDevice)
            is UiAction.OnSendData -> sendData(event.data, event.dataType)
            is UiAction.OnSaveImageClick -> saveImageToGallery(event.imageSaverAction)
        }
    }

    private fun sendData(data: BluetoothData, dataType: DataType) {
        viewModelScope.launch {
            val bluetoothData = bluetoothHandler.tryToSendData(data, dataType)

            if (bluetoothData != null) {
                updateState {
                    copy(
                        data = this.data + bluetoothData
                    )
                }
            }
        }
    }

    private fun connectToDevice(device: FoundBluetoothDevice) {
        bluetoothHandler
            .connectToDevice(device)
            .observe()
    }

    private fun onDisconnectIconClick() {
        bluetoothHandler.cancelClientSocket()
    }

    private fun startBluetoothServer() {
        bluetoothHandler
            .startBluetoothServer()
            .observe()
    }

    private fun updatePairedDevicesState() {
        bluetoothHandler.updatePairedDevices()
    }

    private fun onBluetoothEnableClick() {
        if (bluetoothHandler.isBluetoothAvailable()) bluetoothHandler.enableBluetooth()
    }

    private fun onButtonScanClick(onChangeButtonText: (String) -> Unit) {
        bluetoothHandler.scanForDevices()
        onChangeButtonText.invoke("Stop scanning")
    }

    private fun onButtonStopScanClick(onChangeButtonText: (String) -> Unit) {
        bluetoothHandler.stopScan()
        onChangeButtonText.invoke("Scan")
    }

    private fun pairDevice(device: FoundBluetoothDevice) {
        return bluetoothHandler.pairDevice(device)
    }

    private fun Flow<BluetoothState>.observe(): Job = onEach { bluetoothState ->

        when (bluetoothState) {
            BluetoothState.BluetoothConnectionSuccess -> {
                _connectState.update {
                    it.copy(isConnectedDevice = true)
                }
            }

            is BluetoothState.BluetoothConnectionError -> {
                _connectState.update {
                    it.copy(
                        isConnectedDevice = false,
                        errorMessage = bluetoothState.message
                    )
                }
            }

            is BluetoothState.BluetoothDataTransferSuccess -> {
                updateState {
                    copy(
                        data = this.data + bluetoothState.data
                    )
                }
            }
        }
    }.launchIn(viewModelScope)

    private fun resetTransferredData() {
        updateState {
            copy(
                data = emptyList()
            )
        }
    }

    private fun saveImageToGallery(
        imageSaverAction: suspend () -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        imageSaverAction.invoke()
    }

    private inline fun updateState(
         update: TransferState.() -> TransferState
    ) {
        _transferState.update { currentState ->
            currentState.update()
        }
    }

    class Factory(private val bluetoothHandler: BluetoothHandler) :
        ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(bluetoothHandler) as T
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothHandler.clearBluetoothHandler()
    }
}