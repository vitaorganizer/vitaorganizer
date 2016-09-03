package com.soywiz.vitaorganizer

object FileSize {
    val BYTES = 1L
    val KB = 1024 * BYTES
    val MB = 1024 * KB
    val GB = 1024 * MB
    val TB = 1024 * GB

    fun toString(size: Long): String {
        if (size < KB) return "%.1f B".format(size.toDouble() / BYTES.toDouble())
        if (size < MB) return "%.1f KB".format(size.toDouble() / KB.toDouble())
        if (size < GB) return "%.1f MB".format(size.toDouble() / MB.toDouble())
        if (size < TB) return "%.1f GB".format(size.toDouble() / GB.toDouble())
        return "%.1f TB".format(size.toDouble() / TB.toDouble())
    }
}