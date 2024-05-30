package com.example.tranzfera.data.bluetooth

import android.graphics.Bitmap

data class BluetoothData(
    val message: String? = "",
    val image: Bitmap? = null,
    val sender: String? = null,
    val isFromMySide: Boolean
)

fun String.toBluetoothData(isFromMySide: Boolean): BluetoothData {
    val sender = substringBeforeLast("#")
    val message = substringAfter("#")

    return BluetoothData(
        message = message,
        sender = sender,
        isFromMySide = isFromMySide
    )
}

fun Bitmap.toBluetoothData(isFromMySide: Boolean): BluetoothData {
    return BluetoothData(
        image = this,
//        sender = sender, TODO
        isFromMySide = isFromMySide
    )
}

fun BluetoothData.toByteArray(): ByteArray {
    return if (!message.isNullOrEmpty())
        "$sender#$message".encodeToByteArray()
    else
        "$sender#$image".encodeToByteArray()
}

