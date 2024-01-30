package com.example.tranzfera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class MainViewModel(
    private val bluetoothHandler: BluetoothHandler
) : ViewModel() {


    private val _state = MutableStateFlow(AppState())
    val state = combine(
        bluetoothHandler.scannedDevices,
        bluetoothHandler.pairedDevices,
        bluetoothHandler.connectedDevice,
        bluetoothHandler.isBluetoothEnabled,
        _state
    ) { scannedDevices, pairedDevices, connectedDevice, isBluetoothEnabled, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
            connectedDevice = connectedDevice,
            isBluetoothEnabled = isBluetoothEnabled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun connectToDevice(device: FoundBluetoothDevice) {
        bluetoothHandler
            .connectToDevice(device)
            .observe()
    }

    fun startBluetoothServer() {
        bluetoothHandler
            .startBluetoothServer()
            .observe()
    }

    fun clearBluetoothHandler() {
        bluetoothHandler.clearBluetoothHandler()
    }

    fun onBluetoothEnableClick() {
        if (bluetoothHandler.isBluetoothAvailable()) bluetoothHandler.enableBluetooth()
    }

    fun onButtonScanClick(buttonText: (String) -> Unit) {
        bluetoothHandler.scanForDevices()
        buttonText.invoke("Stop scanning")
    }

    fun onButtonStopScanClick(buttonText: (String) -> Unit) {
        bluetoothHandler.stopScan()
        buttonText.invoke("Scan")
    }

    fun pairDevice(device: FoundBluetoothDevice) {
        return bluetoothHandler.pairDevice(device)
    }

    private fun Flow<BluetoothConnectionState>.observe(): Job {

        return onEach { state ->
            when (state) {
                BluetoothConnectionState.BluetoothConnectionSuccess -> {
                    _state.update {
                        it.copy(
                            isConnectedDevice = true
                        )
                    }
                }

                is BluetoothConnectionState.BluetoothConnectionError -> {
                    _state.update {
                        it.copy(
                            isConnectedDevice = false,
                            errorMessage = state.message
                        )
                    }
                }
            }
        }.catch { _ ->
            bluetoothHandler.cancelBluetoothConnection()
            _state.update {
                it.copy(
                    isConnectedDevice = false
                )
            }
        }.launchIn(viewModelScope)
    }

    class Factory(private val bluetoothHandler: BluetoothHandler) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(bluetoothHandler) as T
        }
    }
}