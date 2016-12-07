package com.soywiz.vitaorganizer

import java.util.*

class FileSize(val value: Long) : Comparable<FileSize> {
	companion object {
		val locale = Locale.ENGLISH
		//val locale = Locale.getDefault()

		private val BYTES = 1L
		private val kB = 1000 * BYTES
		private val MB = 1000 * kB
		private val GB = 1000 * MB
		private val TB = 1000 * GB

		private val KiB = 1024 * BYTES
		private val MiB = 1024 * KiB
		private val GiB = 1024 * MiB
		private val TiB = 1024 * GiB

		val base = VitaOrganizerSettings.unitBase;

		fun getPrecissionFromSize10(size: Long) = if (size < kB) 0 else if (size < MB) 0 else if (size < GB) 1 else if (size < TB) 3 else 6
		fun getPrecissionFromSize2(size: Long) = if (size < KiB) 0 else if (size < MiB) 0 else if (size < GiB) 1 else if (size < TiB) 3 else 6
		fun getPrecissionFromSize(size: Long) = if (base == 10) getPrecissionFromSize10(size) else getPrecissionFromSize2(size)

		fun toString10(size: Long, precission: Int = getPrecissionFromSize10(size)): String {
			if (size < kB) return "%.${precission}f B".format(locale, size.toDouble() / BYTES.toDouble())
			if (size < MB) return "%.${precission}f kB".format(locale, size.toDouble() / kB.toDouble())
			if (size < GB) return "%.${precission}f MB".format(locale, size.toDouble() / MB.toDouble())
			if (size < TB) return "%.${precission}f GB".format(locale, size.toDouble() / GB.toDouble())
			return "%.${precission}f TB".format(locale, size.toDouble() / TB.toDouble())
		}

		fun toString2(size: Long, precission: Int = getPrecissionFromSize2(size)): String {
			if (size < KiB) return "%.${precission}f B".format(locale, size.toDouble() / BYTES.toDouble())
			if (size < MiB) return "%.${precission}f KiB".format(locale, size.toDouble() / KiB.toDouble())
			if (size < GiB) return "%.${precission}f MiB".format(locale, size.toDouble() / MiB.toDouble())
			if (size < TiB) return "%.${precission}f GiB".format(locale, size.toDouble() / GiB.toDouble())
			return "%.${precission}f TiB".format(locale, size.toDouble() / TiB.toDouble())
		}

		fun toString(size: Long, precission: Int = getPrecissionFromSize(size)): String = if (base == 10) toString10(size, precission)
        else toString2(size, precission)
	}

	override fun compareTo(other: FileSize): Int = this.value.compareTo(other.value)
	override fun toString() = FileSize.toString(value)
}
