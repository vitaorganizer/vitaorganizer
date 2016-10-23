package com.soywiz.vitaorganizer

import java.util.*

class FileSize(val value: Long) : Comparable<FileSize> {
	companion object {
		val locale = Locale.ENGLISH
		//val locale = Locale.getDefault()

		private val BYTES = 1L
		private val KB = 1024 * BYTES
		private val MB = 1024 * KB
		private val GB = 1024 * MB
		private val TB = 1024 * GB

		fun getPrecissionFromSize(size: Long) = if (size < KB) 0 else if (size < MB) 0 else if (size < GB) 1 else if (size < TB) 3 else 6

		fun toString(size: Long, precission: Int = getPrecissionFromSize(size)): String {
			if (size < KB) return "%.${precission}f B".format(locale, size.toDouble() / BYTES.toDouble())
			if (size < MB) return "%.${precission}f KB".format(locale, size.toDouble() / KB.toDouble())
			if (size < GB) return "%.${precission}f MB".format(locale, size.toDouble() / MB.toDouble())
			if (size < TB) return "%.${precission}f GB".format(locale, size.toDouble() / GB.toDouble())
			return "%.${precission}f TB".format(locale, size.toDouble() / TB.toDouble())
		}
	}

	override fun compareTo(other: FileSize): Int = this.value.compareTo(other.value)
	override fun toString() = FileSize.toString(value)
}