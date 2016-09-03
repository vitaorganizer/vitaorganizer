package com.soywiz.util

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.nio.charset.Charset

abstract class Stream2 : Closeable {
    var position: Long = 0L
    open val length: Long get() = noImpl
    open val eof: Boolean get() = position >= length
    open val available: Long get() = length - position
    open fun readInternal(position: Long, bytes: ByteArray, offset: Int, count: Int): Int = noImpl
    open fun writeInternal(position: Long, bytes: ByteArray, offset: Int, count: Int): Unit = noImpl

    fun read(bytes: ByteArray, offset: Int = 0, count: Int = bytes.size): Int {
        val readed = readInternal(position, bytes, offset, count)
        position += readed
        return readed
    }

    fun write(bytes: ByteArray, offset: Int, count: Int): Unit {
        writeInternal(position, bytes, offset, count)
        position += count
    }

    private fun _read(count: Int): ByteArray {
        read(temp, 0, count)
        return temp
    }

    private fun _write(bytes: ByteArray, count: Int): Unit {
        write(bytes, 0, count)
    }

    val temp = ByteArray(16)

    fun writeU8(v: Byte): Unit = _write(BitWrite.U8(temp, 0, v.toInt()), 1)
    fun writeU8(v: Int): Unit = _write(BitWrite.U8(temp, 0, v.toInt()), 1)
    fun writeU16_le(v: Char): Unit = _write(BitWrite.U16_le(temp, 0, v.toInt()), 2)
    fun writeU16_le(v: Short): Unit = _write(BitWrite.U16_le(temp, 0, v.toInt()), 2)
    fun writeU16_le(v: Int): Unit = _write(BitWrite.U16_le(temp, 0, v.toInt()), 2)
    fun writeU32_le(v: Int): Unit = _write(BitWrite.U32_le(temp, 0, v.toInt()), 4)
    fun writeU32_le(v: Long): Unit = _write(BitWrite.U32_le(temp, 0, v.toInt()), 4)

    fun writeU16_be(v: Int): Unit = _write(BitWrite.U16_be(temp, 0, v.toInt()), 2)
    fun writeU32_be(v: Int): Unit = _write(BitWrite.U32_be(temp, 0, v.toInt()), 4)

    fun readS8(): Int = BitRead.S8(_read(1), 0)
    fun readS16_le(): Int = BitRead.S16_le(_read(2), 0)
    fun readS24_le(): Int = BitRead.S24_le(_read(3), 0)
    fun readS32_le(): Int = BitRead.S32_le(_read(4), 0)
    fun readU32_le(): Long = BitRead.U32_le(_read(4), 0)

    fun readS64_le(): Long = BitRead.S64_le(_read(8), 0)
    fun readS64_be(): Long = BitRead.S64_be(_read(8), 0)

    fun readS16_be(): Int = java.lang.Short.reverseBytes(BitRead.S32_le(_read(2), 0).toShort()).toInt()
    fun readS32_be(): Int = Integer.reverseBytes(BitRead.S32_le(_read(4), 0))

    fun readU32_be(): Long = Integer.reverseBytes(BitRead.S32_le(_read(4), 0)).toLong() and 0xFFFFFFFFL

    fun copyFrom(from: Stream2): Unit = noImpl

    override fun close() {
    }

    fun readShortArray_le(count: Int): ShortArray {
        val out = ShortArray(count)
        for (n in 0 until count) out[n] = readS16_le().toShort()
        return out
    }

    fun readIntArray_le(count: Int): IntArray {
        val out = IntArray(count)
        for (n in 0 until count) out[n] = readS32_le()
        return out
    }

    fun readLongArray_le(count: Int): LongArray {
        val out = LongArray(count)
        for (n in 0 until count) out[n] = readS64_le()
        return out
    }

    fun readBytes(count: Int): ByteArray {
        val data = ByteArray(count)
        if (count > 0) read(data, 0, count)
        return data
    }

    fun writeBytes(data: ByteArray, offset: Int = 0, size: Int = data.size) = write(data, offset, size)
    fun writeBytes(data: ByteArraySlice) = write(data.data, data.position, data.length)

    fun readU8(): Int = readS8() and 0xFF
    fun readU24_le(): Int = readS24_le() and 0xFFFFFF
    fun readU16_le(): Int = readS16_le() and 0xFFFF

    fun readU16_be(): Int = readS16_be() and 0xFFFF

    fun readStringz(count: Int, charset: Charset = Charsets.UTF_8): String {
        val bytes = readBytes(count)
        val index = bytes.indexOf(0.toByte())
        return bytes.copyOf(if (index >= 0) index else bytes.size).toString(charset)
    }

    fun readStringz(charset: Charset = Charsets.UTF_8): String {
        val out = ByteArrayOutputStream()
        while (!eof) {
            val b = readU8()
            if (b == 0) break
            out.write(b)
        }
        return out.toByteArray().toString(charset)
    }

    private fun _slice(start: Long, end: Long): SliceStream2 {
        if (this is SliceStream2) {
            return SliceStream2(this.parent, this.start + start, this.start + end)
        } else {
            return SliceStream2(this, start, end)
        }
    }

    fun slice(start: Long): SliceStream2 = _slice(start, this.length)
    fun slice(range: LongRange): SliceStream2 = _slice(range.start, range.endInclusive + 1)

    fun slice(start: Int): SliceStream2 = _slice(start.toLong(), this.length)
    fun slice(range: IntRange): SliceStream2 = _slice(range.start.toLong(), range.endInclusive.toLong() + 1)

    inline fun <T> keepPosition(callback: () -> T): T {
        val oldPos = this.position
        try {
            return callback()
        } finally {
            this.position = oldPos
        }
    }

    fun writeStringz(text: String, count: Int, charset: Charset = Charsets.UTF_8) {
        val bytes = text.toByteArray(charset)
        writeBytes(bytes.copyOf(count))
    }

    fun writeStringz(text: String, charset: Charset = Charsets.UTF_8) {
        val bytes = text.toByteArray(charset)
        writeBytes(bytes.copyOf(bytes.size + 1))
    }

    fun writeStream(data: Stream2) {
        val temp = ByteArray(0x10000)
        while (!data.eof) {
            val toRead = Math.min(temp.size.toLong(), data.available).toInt()
            val readed = data.read(temp, 0, toRead)
            this.write(temp, 0, readed)
        }
    }

    fun readStream(count: Int): Stream2 {
        val out = this.slice(this.position until this.position + count)
        this.position += count
        return out
    }

    fun readAll(): ByteArray = readBytes(available.toInt())

    fun slice(): Stream2 = slice(position)

    var bitsData: Int = 0
    var availableBits: Int = 0

    private fun addMoreBitsHigh() {
        bitsData = bitsData or (readU8() shl (24 - availableBits))
        availableBits += 8
    }

    fun readBitsHigh(count: Int): Int {
        if (count > 32) invalidOp("Can't read more than 32 bits")
        var out = 0
        var outOffset = 0
        var pendingBits = count
        while (pendingBits > 0) {
            if (availableBits <= 0) addMoreBitsHigh()
            val readBits = Math.min(pendingBits, availableBits)

            val chunk = bitsData ushr (32 - readBits)
            out = out or (chunk shl outOffset)

            //println("readedBits:$readBits")

            bitsData = bitsData shl readBits
            availableBits -= readBits
            outOffset += readBits
            pendingBits -= readBits
        }
        return out
    }

    private fun addMoreBitsLow() {
        bitsData = bitsData or (readU8() shl availableBits)
        availableBits += 8
    }

    fun readBitsLow(count: Int): Int {
        if (count > 32) invalidOp("Can't read more than 32 bits")
        var out = 0
        var outOffset = 0
        var pendingBits = count
        while (pendingBits > 0) {
            if (availableBits <= 0) addMoreBitsLow()
            val readBits = Math.min(pendingBits, availableBits)

            val chunk = bitsData and ((1 shl readBits) - 1)
            out = out or (chunk shl outOffset)

            //println("readedBits:$readBits")

            bitsData = bitsData ushr readBits
            availableBits -= readBits
            outOffset += readBits
            pendingBits -= readBits
        }
        return out
    }

    fun alignBits() {
        bitsData = 0
        availableBits = 0
    }

    fun writeToAlign(alignment: Int, value: Int = 0) {
        while ((position % alignment) != 0L) {
            writeU8(value)
        }
    }

    /*
    var bit_window: Int = 0
    var bits_in_window: Int = 0

    private fun show_bits(bit_count: Int): Int {
        return bit_window shr 24 - bit_count
    }

    fun get_bits(bit_count: Int): Int {
        val result = show_bits(bit_count)
        bit_window = (bit_window shl bit_count) and 0xFFFFFF
        bits_in_window -= bit_count
        while (bits_in_window < 16) {
            bit_window = bit_window or (readU8() shl 16 - bits_in_window)
            bits_in_window += 8
        }
        return result
    }
    */
}
