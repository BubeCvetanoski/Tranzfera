package com.example.tranzfera

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import com.example.tranzfera.util.HelperFunctions.parcelable
import com.example.tranzfera.util.HelperFunctions.requestPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothHandler(
    private val context: Context,
    private val intentLauncher: ActivityResultLauncher<Intent>,
    permissionLauncher: ActivityResultLauncher<Array<String>>
) {
    private val _scannedDevices = MutableStateFlow<List<FoundBluetoothDevice>>(listOf())
    val scannedDevices: StateFlow<List<FoundBluetoothDevice>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<FoundBluetoothDevice>>(listOf())
    val pairedDevices: StateFlow<List<FoundBluetoothDevice>>
        get() = _pairedDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<FoundBluetoothDevice?>(null)
    val connectedDevice: StateFlow<FoundBluetoothDevice?>
        get() = _connectedDevice.asStateFlow()

    private var _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean>
        get() = _isBluetoothEnabled.asStateFlow()

    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null

    private val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val foundDevice: BluetoothDevice? =
                        intent.parcelable(BluetoothDevice.EXTRA_DEVICE)

                    foundDevice?.let {
                        val newDevice = it.toFoundBluetoothDevice()

                        _scannedDevices.update { existingDevices ->
                            if (newDevice in existingDevices) existingDevices else existingDevices + newDevice
                        }
                    }
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )) {
                        BluetoothAdapter.STATE_OFF -> {
                            _isBluetoothEnabled.value = false
                        }

                        BluetoothAdapter.STATE_ON -> {
                            _isBluetoothEnabled.value = true
                        }
                    }
                }

                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val foundDevice: BluetoothDevice? =
                        intent.parcelable(BluetoothDevice.EXTRA_DEVICE)

                    foundDevice?.let {
                        val newConnectedDevice = it.toFoundBluetoothDevice()

                        _connectedDevice.update { newConnectedDevice }
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    _connectedDevice.update { null }
                }
            }
        }
    }

    init {
        permissionLauncher.requestPermissions()
        checkBluetoothEnabled()
        updatePairedDevices()
        registerBluetoothStateReceiver()
        registerConnectedDeviceReceiver()
    }

    private fun checkBluetoothEnabled() {
        _isBluetoothEnabled.value = isBluetoothEnabled() == true
    }

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    private fun isBluetoothEnabled(): Boolean? = bluetoothAdapter?.isEnabled

    fun enableBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            intentLauncher.launch(enableBtIntent)
        }
    }

    fun scanForDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                return
            }
        } else {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                return
            }
        }
        registerBluetoothScanReceiver()

        bluetoothAdapter?.startDiscovery()
    }

    fun stopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                return
            }
        } else {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                return
            }
        }

        bluetoothAdapter?.cancelDiscovery()
    }

    private fun registerBluetoothScanReceiver() {
        context.registerReceiver(
            bluetoothReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
    }

    private fun registerBluetoothStateReceiver() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothReceiver, filter)
    }

    private fun registerConnectedDeviceReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }

    private fun unregisterBluetoothReceiver() {
        context.unregisterReceiver(bluetoothReceiver)
    }

    fun pairDevice(device: FoundBluetoothDevice) {
        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
        bluetoothDevice?.createBond()

        updatePairedDevices()
    }

    private fun updatePairedDevices() {
        bluetoothAdapter
            ?.bondedDevices
            ?.map { it.toFoundBluetoothDevice() }
            ?.also { devices ->
                _pairedDevices.update { devices }
            }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    fun connectToDevice(device: FoundBluetoothDevice): Flow<BluetoothConnectionState> {
        return flow {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    throw SecurityException("No BLUETOOTH_CONNECT permission")
                }
            }
            clientSocket = bluetoothAdapter
                ?.getRemoteDevice(device.address)
                ?.createRfcommSocketToServiceRecord(
                    UUID.fromString(UNIQUE_UUID)
                )

            if (bluetoothAdapter!!.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

            clientSocket?.let { currentClientSocket ->
                try {
                    currentClientSocket.connect()
                    emit(BluetoothConnectionState.BluetoothConnectionSuccess)

                } catch (e: IOException) {
                    currentClientSocket.close()
                    clientSocket = null
                    emit(BluetoothConnectionState.BluetoothConnectionError("Connection is not successful, try again!"))
                }
            }
        }.onCompletion {
            cancelClientSocket()
        }.flowOn(Dispatchers.IO)
    }

    fun startBluetoothServer(): Flow<BluetoothConnectionState> {
        return flow {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    throw SecurityException("No BLUETOOTH_CONNECT permission")
                }
            }

            serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                context.packageName + " service",
                UUID.fromString(UNIQUE_UUID)
            )

            var shouldLoop = true

            while (shouldLoop) {
                clientSocket = try {
                    serverSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }
                emit(BluetoothConnectionState.BluetoothConnectionSuccess)
            }
        }.onCompletion {
            cancelServerSocket()
            cancelClientSocket()
        }.flowOn(Dispatchers.IO)
    }

    private fun cancelServerSocket() {
        serverSocket?.close()
        serverSocket = null
    }

    private fun cancelClientSocket() {
        clientSocket?.close()
        clientSocket = null
    }

    fun cancelBluetoothConnection() {
        cancelServerSocket()
        cancelClientSocket()
    }

    fun clearBluetoothHandler() {
        cancelBluetoothConnection()
        unregisterBluetoothReceiver()
    }

    companion object {
        const val UNIQUE_UUID = "b00d8a66-5f78-417c-bab0-311125551a79"
    }
}