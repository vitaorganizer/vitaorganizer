package util

import com.soywiz.util.Bitmap
import java.io.File

open class ImageFormat {
	open fun check(s: Stream2): Boolean = noImpl
	open fun read(s: Stream2): Bitmap = noImpl
	open fun write(bitmap: Bitmap, s: Stream2): Unit = noImpl
}

object ImageFormats : ImageFormat() {
	private val formats = listOf(PNG, JPEG, BMP, TGA)

	override fun check(s: Stream2): Boolean {
		for (format in formats) if (format.check(s.slice())) return true
		return false
	}

	override fun read(s: Stream2): Bitmap {
		for (format in formats) {
			if (format.check(s.slice())) {
				return format.read(s.slice())
			}
		}
		invalidOp("Not suitable image format")
	}
}

fun ImageFormat.read(file: File) = this.read(file.open2("r"))
fun ImageFormat.encode(bitmap: Bitmap): ByteArray {
	val mem = MemoryStream2(byteArrayOf())
	write(bitmap, mem)
	return mem.toByteArray()
}