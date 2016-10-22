package com.soywiz.vitaorganizer

class FileSize(val value: Long) : Comparable<FileSize> {
	companion object {
		private val BYTES = 1L
		private val KB = 1024 * BYTES
		private val MB = 1024 * KB
		private val GB = 1024 * MB
		private val TB = 1024 * GB

		fun getPrecissionFromSize(size: Long) = if (size < KB) 0 else if (size < MB) 0 else if (size < GB) 1 else if (size < TB) 3 else 6

		fun toString(size: Long, precission: Int = getPrecissionFromSize(size)): String {
			if (size < KB) return "%.${precission}f B".format(size.toDouble() / BYTES.toDouble())
			if (size < MB) return "%.${precission}f KB".format(size.toDouble() / KB.toDouble())
			if (size < GB) return "%.${precission}f MB".format(size.toDouble() / MB.toDouble())
			if (size < TB) return "%.${precission}f GB".format(size.toDouble() / GB.toDouble())
			return "%.${precission}f TB".format(size.toDouble() / TB.toDouble())
		}
	}

	override fun compareTo(other: FileSize): Int = this.value.compareTo(other.value)
	override fun toString() = FileSize.toString(value)
}