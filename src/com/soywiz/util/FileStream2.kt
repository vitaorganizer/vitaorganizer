package com.soywiz.util

import java.io.File
import java.io.RandomAccessFile

class FileStream2(val file: File, val mode: String) : Stream2() {
    val s = RandomAccessFile(file, mode)
    override val length: Long get() = s.length()
    override fun readInternal(position: Long, bytes: ByteArray, offset: Int, count: Int): Int {
        s.seek(position)
        return s.read(bytes, offset, count)
    }

    override fun writeInternal(position: Long, bytes: ByteArray, offset: Int, count: Int) {
        s.seek(position)
        s.write(bytes, offset, count)
    }
}