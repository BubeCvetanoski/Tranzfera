package com.example.tranzfera.data.bluetooth

import android.bluetooth.BluetoothSocket
import com.example.tranzfera.util.HelperFunctions.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException

class BluetoothDataTransferService(
    private val socket: BluetoothSocket
) {
    fun listenForIncomingData(): Flow<BluetoothData> {
        return flow {
            if (!socket.isConnected) {
                return@flow
            }
            val buffer = ByteArray(1024)

            while (true) {
                try {
                    val byteCount = socket.inputStream.read(buffer)
                    if (byteCount > 0) {
                        val dataType = buffer[0] // The first byte indicates the data type
                        val dataContent = buffer.copyOfRange(1, byteCount) // Remove the prefix

                        when (dataType) {
                            DataType.StringType.value -> {
                                val stringData =
                                    dataContent.decodeToString(endIndex = dataContent.size)

                                if (stringData.isNotBlank()) {
                                    emit(stringData.toBluetoothData(isFromMySide = false))
                                }
                            }

                            DataType.ImageType.value -> {
                                val imageData = dataContent.toBitmap()

                                if (imageData != null) {
                                    emit(imageData.toBluetoothData(isFromMySide = false))
                                }
                            }

                            else -> {}
                        }
                    }
                } catch (e: IOException) {
                    throw IOException("Data transfer failed. Please try again!", e)
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun sendData(data: ByteArray, dataType: DataType): Boolean =
        withContext(Dispatchers.IO) {
            try {
                socket.outputStream.write(byteArrayOf(dataType.value) + data)
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext false
            }
            true
        }
}