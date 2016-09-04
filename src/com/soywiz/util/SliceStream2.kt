package com.soywiz.util

class SliceStream2(
        internal val parent: Stream2,
        internal val start: Long,
        internal val end: Long
) : Stream2() {
    override val length: Long get() = end - start

    override fun readInternal(position: Long, bytes: ByteArray, offset: Int, count: Int): Int {
        return parent.readInternal(this.start + position, bytes, offset, count)
    }

    override fun writeInternal(position: Long, bytes: ByteArray, offset: Int, count: Int) {
        parent.writeInternal(this.start + position, bytes, offset, count)
    }
}