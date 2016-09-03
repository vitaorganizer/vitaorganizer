package com.soywiz.util

class MemoryStream2() : Stream2() {
    private var data = ByteArray(1024)
    override var length: Long = 0L

    constructor(data: ByteArray, size: Int = data.size) : this() {
        this.data = data.copyOf()
        this.length = size.toLong()
    }

    fun toByteArray(): ByteArray = data.copyOf(length.toInt())
    fun toByteArraySliceFromPos(): ByteArraySlice = ByteArraySlice(data, position.toInt(), available.toInt())
    fun toByteArraySlice(): ByteArraySlice = ByteArraySlice(data, 0.toInt(), length.toInt())


    private fun ensureLength(length: Long) {
        if (this.data.size < length) {
            this.data = this.data.copyOf(Math.max(length.toInt(), this.data.size * 0x10))
        }
        this.length = Math.max(this.length, length)
    }

    override fun readInternal(position: Long, bytes: ByteArray, offset: Int, count: Int): Int {
        val available = this.length - position
        val readed = Math.min(available, count.toLong()).toInt()
        System.arraycopy(this.data, position.toInt(), bytes, offset, readed)
        return readed
    }

    override fun writeInternal(position: Long, bytes: ByteArray, offset: Int, count: Int) {
        ensureLength(position + count)
        System.arraycopy(bytes, offset, this.data, position.toInt(), count)
    }

    override fun toString(): String = "MemoryStream2($length)"
}