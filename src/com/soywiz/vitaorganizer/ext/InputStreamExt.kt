package com.soywiz.vitaorganizer.ext

import java.io.InputStream
import java.io.OutputStream

fun InputStream.copyToReport(out: OutputStream, length: Long = this.available().toLong(), report: (current: Long, total: Long, chunk: Int) -> Unit = { current, total, chunk -> }) {
	val temp = ByteArray(0x10000)
	var written = 0L
	report(0, length, 0)
	while (this.available() > 0) {
		val tryRead = Math.min(this.available(), temp.size)
		val read = this.read(temp, 0, tryRead)
		written += read
		out.write(temp, 0, read)
		report(written, length, read)
	}
}