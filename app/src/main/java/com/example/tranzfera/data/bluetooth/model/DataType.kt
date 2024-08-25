package com.example.tranzfera.data.bluetooth.model

enum class DataType(val value: Byte) {
    StringType(0x01),
    ImageType(0x02);

    companion object {
        fun fromValue(value: Byte): DataType? {
            return entries.find { it.value == value }
        }
    }
}