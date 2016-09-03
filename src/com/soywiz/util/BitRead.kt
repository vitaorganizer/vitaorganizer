package com.soywiz.util

object BitRead {
    fun Int.signExtend(bits: Int): Int = (this shl (32 - bits)) shr (32 - bits)

    fun pack16(a: Int, b: Int) = ((a and 0xFF) shl 0) or ((b and 0xFF) shl 8)
    fun pack24(a: Int, b: Int, c: Int) = ((a and 0xFF) shl 0) or ((b and 0xFF) shl 8) or ((c and 0xFF) shl 16)
    fun pack32(a: Int, b: Int, c: Int, d: Int) = ((a and 0xFF) shl 0) or ((b and 0xFF) shl 8) or ((c and 0xFF) shl 16) or ((d and 0xFF) shl 24)
    fun pack64(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int) = pack32(a, b, c, d).toLong() or pack32(e, f, g, h).toLong().shl(32)

    fun S8(bytes: ByteArray, i: Int): Int = bytes[i].toInt()
    fun S16_le(bytes: ByteArray, i: Int): Int = pack16(S8(bytes, i + 0), S8(bytes, i + 1)).toShort().toInt()
    fun S24_le(bytes: ByteArray, i: Int): Int = pack24(S8(bytes, i + 0), S8(bytes, i + 1), S8(bytes, i + 2)).signExtend(24)
    fun S32_le(bytes: ByteArray, i: Int): Int = pack32(S8(bytes, i + 0), S8(bytes, i + 1), S8(bytes, i + 2), S8(bytes, i + 3)).toInt()
    fun S64_le(bytes: ByteArray, i: Int): Long = pack64(S8(bytes, i + 0), S8(bytes, i + 1), S8(bytes, i + 2), S8(bytes, i + 3), S8(bytes, i + 4), S8(bytes, i + 5), S8(bytes, i + 6), S8(bytes, i + 7))

    fun S16_be(bytes: ByteArray, i: Int): Int = Integer.reverseBytes(S16_le(bytes, i))
    fun S32_be(bytes: ByteArray, i: Int): Int = Integer.reverseBytes(S32_le(bytes, i))
    fun S64_be(bytes: ByteArray, i: Int): Long = java.lang.Long.reverseBytes(S64_le(bytes, i))

    fun U8(bytes: ByteArray, i: Int): Int = bytes[i].toInt() and 0xFF
    fun U16_le(bytes: ByteArray, i: Int): Int = S16_le(bytes, i) and 0xFFFF
    fun U32_le(bytes: ByteArray, i: Int): Long = S32_le(bytes, i).toLong() and 0xFFFFFFFFL
    fun U64_le(bytes: ByteArray, i: Int): Long = S64_le(bytes, i).toLong()
}