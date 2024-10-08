package com.example.tranzfera.data.bluetooth

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
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.example.tranzfera.data.bluetooth.model.BluetoothData
import com.example.tranzfera.data.bluetooth.model.DataType
import com.example.tranzfera.data.bluetooth.model.FoundBluetoothDevice
import com.example.tranzfera.data.bluetooth.model.toByteArray
import com.example.tranzfera.data.bluetooth.model.toFoundBluetoothDevice
import com.example.tranzfera.util.HelperFunctions.parcelable
import com.example.tranzfera.util.HelperFunctions.requestPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
    val scannedDevices = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<FoundBluetoothDevice>>(listOf())
    val pairedDevices = _pairedDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<FoundBluetoothDevice?>(null)
    val connectedDevice = _connectedDevice.asStateFlow()

    private var _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled = _isBluetoothEnabled.asStateFlow()

    private var isPairing: Boolean = false

    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null

    private var dataTransferService: BluetoothDataTransferService? = null

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, getIntent: Intent?) {
            getIntent?.let { intent ->
                when (intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        when (intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        )) {
                            BluetoothAdapter.STATE_ON -> {
                                _isBluetoothEnabled.value = true
                            }

                            BluetoothAdapter.STATE_OFF -> {
                                _isBluetoothEnabled.value = false
                            }

                            else -> {}
                        }
                    }

                    BluetoothDevice.ACTION_FOUND -> {
                        val foundDevice: BluetoothDevice? =
                            intent.parcelable(BluetoothDevice.EXTRA_DEVICE)

                        foundDevice?.let {
                            val newDevice = it.toFoundBluetoothDevice()

                            _scannedDevices.update { existingDevices ->
                                if (newDevice in existingDevices) existingDevices
                                else existingDevices + newDevice
                            }
                        }
                    }

                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        val bondState = intent.getIntExtra(
                            BluetoothDevice.EXTRA_BOND_STATE,
                            BluetoothDevice.BOND_NONE
                        )

                        when (bondState) {
                            BluetoothDevice.BOND_BONDED -> {
                                isPairing = false
                                updatePairedDevices()
                            }

                            BluetoothDevice.BOND_BONDING -> {
                                isPairing = true
                            }

                            BluetoothDevice.BOND_NONE -> {
                                isPairing = false
                            }

                            else -> {}
                        }
                    }

                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        if (!isPairing) {
                            val foundDevice: BluetoothDevice? =
                                intent.parcelable(BluetoothDevice.EXTRA_DEVICE)

                            foundDevice?.let {
                                val newConnectedDevice = it.toFoundBluetoothDevice()
                                _connectedDevice.update { newConnectedDevice }
                            }
                        } else {
                            isPairing = false
                        }
                    }

                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        _connectedDevice.update { null }
                    }

                    else -> {}
                }
            }
        }
    }

    init {
        permissionLauncher.requestPermissions()
        checkBluetoothEnabled()
        updatePairedDevices()
        registerBluetoothDevice()
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

    private fun registerBluetoothDevice() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }

    private fun unregisterBluetoothReceiver() {
        context.unregisterReceiver(bluetoothReceiver)
    }

    fun pairDevice(device: FoundBluetoothDevice) {
        isPairing = true
        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
        bluetoothDevice?.createBond()
    }

    fun updatePairedDevices() {
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

    suspend fun tryToSendData(
        data: BluetoothData,
        dataType: DataType
    ): BluetoothData? {

        if (dataTransferService == null) {
            return null
        }

        val bluetoothData: BluetoothData = when (dataType) {
            DataType.StringType -> {
                BluetoothData(
                    message = data.message,
                    sender = bluetoothAdapter?.name ?: "Unknown name",
                    isFromMySide = data.isFromMySide
                )
            }

            DataType.ImageType -> {
                BluetoothData(
                    imageBytes = data.imageBytes,
                    sender = bluetoothAdapter?.name ?: "Unknown name",
                    isFromMySide = data.isFromMySide
                )
            }
        }

        dataTransferService?.sendData(
            data = bluetoothData.toByteArray(dataType),
            dataType = dataType
        )

        return bluetoothData
    }

    @SuppressLint("DiscouragedPrivateApi")
    fun connectToDevice(
        device: FoundBluetoothDevice
    ): Flow<BluetoothState> {
        return flow {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    throw SecurityException("No BLUETOOTH_CONNECT permission")
                }
            }

            val remoteDevice = bluetoothAdapter?.getRemoteDevice(device.address)
            clientSocket = remoteDevice?.createRfcommSocketToServiceRecord(
                UUID.fromString(UNIQUE_UUID)
            )

            if (clientSocket == null) {
                emit(BluetoothState.BluetoothConnectionError("Client socket initialization failed"))
                return@flow
            }

            if (bluetoothAdapter!!.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

            try {
                val socketField = clientSocket
                    ?.javaClass
                    ?.getDeclaredField("mSocket")
                    ?.apply {
                        isAccessible = true
                    }
                val socket = socketField?.get(clientSocket) as? java.net.Socket
                socket?.soTimeout = 100000

                clientSocket?.connect()
                emit(BluetoothState.BluetoothConnectionSuccess)

                val service = BluetoothDataTransferService(socket = clientSocket!!)
                dataTransferService = service

                emitAll(
                    service.listenForIncomingData().map { data ->
                        BluetoothState.BluetoothDataTransferSuccess(data)
                    }
                )

            } catch (e: IOException) {
                cancelClientSocket()
                emit(
                    BluetoothState.BluetoothConnectionError("Connection is not successful, try again!")
                )
            }
        }.flowOn(Dispatchers.IO).catch { e ->
            emit(BluetoothState.BluetoothConnectionError(e.message ?: "Unknown error"))
        }
    }

    fun startBluetoothServer(): Flow<BluetoothState> {
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

            if (serverSocket == null) {
                emit(
                    BluetoothState.BluetoothConnectionError("Server socket initialization failed")
                )
                return@flow
            }

            var shouldLoop = true

            while (shouldLoop) {
                clientSocket = try {
                    serverSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }
                if (clientSocket != null) {
                    emit(BluetoothState.BluetoothConnectionSuccess)

                    val service = BluetoothDataTransferService(socket = clientSocket!!)
                    dataTransferService = service

                    emitAll(
                        service.listenForIncomingData().map { data ->
                            BluetoothState.BluetoothDataTransferSuccess(data)
                        }
                    )
                }
            }
        }.flowOn(Dispatchers.IO)
            .catch { e ->
                emit(BluetoothState.BluetoothConnectionError(e.message ?: "Unknown error"))
            }
    }

    private fun cancelServerSocket() {
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e("BluetoothConnection", "Error closing server socket: ${e.localizedMessage}", e)
        } finally {
            serverSocket = null
        }
    }

    fun cancelClientSocket() {
        try {
            clientSocket?.close()
        } catch (e: IOException) {
            Log.e("BluetoothConnection", "Error closing client socket: ${e.localizedMessage}", e)
        } finally {
            clientSocket = null
        }
    }

    private fun cancelBluetoothConnection() {
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