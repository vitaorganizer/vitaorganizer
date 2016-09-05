package com.soywiz.vitaorganizer

class FileSize(val value: Long) : Comparable<FileSize> {
    companion object {
        private val BYTES = 1L
        private val KB = 1024 * BYTES
        private val MB = 1024 * KB
        private val GB = 1024 * MB
        private val TB = 1024 * GB

        fun toString(size: Long): String {
            if (size < KB) return "%.1f B".format(size.toDouble() / BYTES.toDouble())
            if (size < MB) return "%.1f KB".format(size.toDouble() / KB.toDouble())
            if (size < GB) return "%.1f MB".format(size.toDouble() / MB.toDouble())
            if (size < TB) return "%.1f GB".format(size.toDouble() / GB.toDouble())
            return "%.1f TB".format(size.toDouble() / TB.toDouble())
        }
    }

    override fun compareTo(other: FileSize): Int = this.value.compareTo(other.value)
    override fun toString() = FileSize.toString(value)
}