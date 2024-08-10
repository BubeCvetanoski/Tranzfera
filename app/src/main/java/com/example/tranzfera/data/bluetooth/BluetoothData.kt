package com.example.tranzfera.data.bluetooth

data class BluetoothData(
    val message: String? = "",
    val imageBytes: ByteArray? = null,
    val sender: String? = null,
    val isFromMySide: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BluetoothData

        if (message != other.message) return false
        if (imageBytes != null) {
            if (other.imageBytes == null) return false
            if (!imageBytes.contentEquals(other.imageBytes)) return false
        } else if (other.imageBytes != null) return false
        if (sender != other.sender) return false
        if (isFromMySide != other.isFromMySide) return false

        return true
    }

    override fun hashCode(): Int {
        var result = message?.hashCode() ?: 0
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (sender?.hashCode() ?: 0)
        result = 31 * result + isFromMySide.hashCode()
        return result
    }
}

fun String.toBluetoothData(isFromMySide: Boolean): BluetoothData {
    val sender = substringBeforeLast("#")
    val message = substringAfter("#")

    return BluetoothData(
        message = message,
        sender = sender,
        isFromMySide = isFromMySide
    )
}

fun ByteArray.toBluetoothData(isFromMySide: Boolean): BluetoothData {
    return BluetoothData(
        imageBytes = this,
//        sender = sender, TODO
        isFromMySide = isFromMySide
    )
}

fun BluetoothData.toByteArray(dataType: DataType): ByteArray {
    return when (dataType) {
        DataType.StringType -> {
            "$sender#$message".encodeToByteArray()
        }

        DataType.ImageType -> {
            imageBytes ?: byteArrayOf()
        }
    }
}