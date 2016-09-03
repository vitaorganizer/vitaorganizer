package com.soywiz.util

object BitWrite {
    fun U8(bytes: ByteArray, i: Int, v: Int): ByteArray {
        bytes[i] = v.toByte()
        return bytes
    }

    fun U16_le(bytes: ByteArray, i: Int, v: Int): ByteArray {
        bytes[i + 0] = ((v ushr 0) and 0xFF).toByte()
        bytes[i + 1] = ((v ushr 8) and 0xFF).toByte()
        return bytes
    }

    fun U32_le(bytes: ByteArray, i: Int, v: Int): ByteArray {
        bytes[i + 0] = ((v ushr 0) and 0xFF).toByte()
        bytes[i + 1] = ((v ushr 8) and 0xFF).toByte()
        bytes[i + 2] = ((v ushr 16) and 0xFF).toByte()
        bytes[i + 3] = ((v ushr 24) and 0xFF).toByte()
        return bytes
    }

    fun U16_be(bytes: ByteArray, i: Int, v: Int): ByteArray {
        bytes[i + 1] = ((v ushr 0) and 0xFF).toByte()
        bytes[i + 0] = ((v ushr 8) and 0xFF).toByte()
        return bytes
    }

    fun U32_be(bytes: ByteArray, i: Int, v: Int): ByteArray {
        bytes[i + 3] = ((v ushr 0) and 0xFF).toByte()
        bytes[i + 2] = ((v ushr 8) and 0xFF).toByte()
        bytes[i + 1] = ((v ushr 16) and 0xFF).toByte()
        bytes[i + 0] = ((v ushr 24) and 0xFF).toByte()
        return bytes
    }
}