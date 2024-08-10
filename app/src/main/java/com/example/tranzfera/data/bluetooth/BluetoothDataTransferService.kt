package com.example.tranzfera.data.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer

class BluetoothDataTransferService(
    private val socket: BluetoothSocket
) {

    fun listenForIncomingData(): Flow<BluetoothData> {
        return flow {
            if (!socket.isConnected) {
                return@flow
            }

            while (true) {
                try {
                    val typeBuffer = ByteArray(1)
                    socket.inputStream.readFully(typeBuffer)
                    val dataType = typeBuffer[0]

                    if (dataType == DataType.ImageType.value) {
                        val sizeBuffer = ByteArray(4)
                        socket.inputStream.readFully(sizeBuffer)
                        val imageSize = ByteBuffer.wrap(sizeBuffer).int

                        val imageBuffer = ByteArray(imageSize)
                        var totalBytesRead = 0

                        while (totalBytesRead < imageSize) {
                            val bytesRead = socket.inputStream.read(
                                imageBuffer,
                                totalBytesRead,
                                imageSize - totalBytesRead
                            )
                            if (bytesRead == -1) break
                            totalBytesRead += bytesRead
                        }

                        if (totalBytesRead == imageSize) {
                            emit(imageBuffer.toBluetoothData(isFromMySide = false))
                        }
                    } else if (dataType == DataType.StringType.value) {
                        val stringBuffer = ByteArray(1024)
                        val stringBytesRead = socket.inputStream.read(stringBuffer)

                        if (stringBytesRead > 0) {
                            val stringData = stringBuffer.copyOf(stringBytesRead).decodeToString()
                            emit(stringData.toBluetoothData(isFromMySide = false))
                        }
                    } else {
                        Log.e("DataError", "Unknown data type received: $dataType")
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
                val dataSize = data.size
                val imageBuffer = ByteBuffer.allocate(4).putInt(dataSize).array()
                socket.outputStream.write(byteArrayOf(dataType.value) + imageBuffer + data)
                socket.outputStream.flush()
                Log.i("BluetoothDataTransfer", "Sending image data size: ${data.size}")
                Log.i("BluetoothDataTransfer", "Received image data size: ${imageBuffer.size}")

            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext false
            }
            true
        }


    private fun java.io.InputStream.readFully(
        buffer: ByteArray,
        offset: Int = 0,
        length: Int = buffer.size
    ) {
        var bytesRead: Int
        var totalBytesRead = 0

        while (totalBytesRead < length) {
            bytesRead = this.read(buffer, offset + totalBytesRead, length - totalBytesRead)
            if (bytesRead == -1) throw IOException("End of stream reached before reading fully")
            totalBytesRead += bytesRead
        }
    }
}