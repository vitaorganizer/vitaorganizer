package util

import com.soywiz.util.Bitmap
import com.soywiz.util.Bitmap32
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterInputStream
import java.util.zip.InflaterInputStream

object PNG : ImageFormat() {
	const val MAGIC1 = 0x89504E47.toInt()
	const val MAGIC2 = 0x0D0A1A0A.toInt()

	override fun check(s: Stream2): Boolean {
		val magic = s.readS32_be()
		return magic == MAGIC1
	}

	override fun write(bitmap: Bitmap, s: Stream2) {
		val width = bitmap.width
		val height = bitmap.height
		if (bitmap !is Bitmap32) invalidOp("Just supported Bitmap32!")
		s.writeU32_be(MAGIC1)
		s.writeU32_be(MAGIC2)

		fun writeChunk(name: String, data: ByteArray) {
			val nameBytes = name.toByteArray().copyOf(4)

			val crc = CRC32()
			crc.update(nameBytes)
			crc.update(data)

			s.writeU32_be(data.size)
			s.writeBytes(nameBytes)
			s.writeBytes(data)
			s.writeU32_be(crc.value.toInt()) // crc32!
		}

		val deflater = Deflater(1)

		fun compress(data: ByteArray): ByteArray {
			return DeflaterInputStream(ByteArrayInputStream(data), deflater).readBytes()
		}

		fun writeChunk(name: String, callback: Stream2.() -> Unit) {
			return writeChunk(name, MemoryStream2().apply { callback() }.toByteArray())
		}

		fun writeChunkCompressed(name: String, callback: Stream2.() -> Unit) {
			return writeChunk(name, compress(MemoryStream2().apply { callback() }.toByteArray()))
		}

		writeChunk("IHDR") {
			writeU32_be(width)
			writeU32_be(height)
			writeU8(8) // bits
			writeU8(6) // colorspace
			writeU8(0) // compressionmethod
			writeU8(0) // filtermethod
			writeU8(0) // interlacemethod
		}

		writeChunkCompressed("IDAT") {
			for (y in 0 until height) {
				writeU8(0) // no filter
				for (x in 0 until width) {
					val c = bitmap[x, y]
					writeU8(RGBA.getB(c))
					writeU8(RGBA.getG(c))
					writeU8(RGBA.getR(c))
					writeU8(RGBA.getA(c))
				}
			}
		}

		writeChunk("IEND") {
		}
	}

	override fun read(s: Stream2): Bitmap {
		if (s.readS32_be() != MAGIC1) invalidOp("Invalid PNG file")
		s.readS32_be() // magic continuation

		class Header(
			val width: Int,
			val height: Int,
			val bits: Int,
			val colorspace: Int,
			val compressionmethod: Int,
			val filtermethod: Int,
			val interlacemethod: Int
		) {
			val bytes = when (colorspace) {
				2 -> 3
				6 -> 4
				else -> 1
			}
		}

		var header = Header(0, 0, 0, 0, 0, 0, 0)
		val pngdata = ByteArrayOutputStream()

		fun Stream2.readChunk() {
			val length = readS32_be()
			val type = readStringz(4)
			val data = readStream(length)
			val crc = readS32_be()

			when (type) {
				"IHDR" -> {
					header = data.run {
						Header(
							width = readS32_be(),
							height = readS32_be(),
							bits = readU8(),
							colorspace = readU8(),
							compressionmethod = readU8(),
							filtermethod = readU8(),
							interlacemethod = readU8()
						)
					}
				}
				"IDAT" -> {
					pngdata.write(data.readAll())
				}
				"IEND" -> {
				}
			}
			//println(type)
		}

		while (!s.eof) {
			s.readChunk()
		}

		val data = InflaterInputStream(ByteArrayInputStream(pngdata.toByteArray())).readBytes().open2("r")

		var lastRow = UByteArray(header.width * header.bytes)
		var currentRow = UByteArray(header.width * header.bytes)
		val row = IntArray(header.width)

		val out = Bitmap32(header.width, header.height)

		for (y in 0 until header.height) {
			val filter = data.readU8()
			data.read(currentRow.data, 0, header.width * header.bytes)
			applyFilter(filter, lastRow, currentRow, header.bytes)
			when (header.bytes) {
				3 -> {
					var m = 0
					for (n in 0 until header.width) {
						val r = currentRow[m++]
						val g = currentRow[m++]
						val b = currentRow[m++]
						row[n] = RGBA.packFast(r, g, b, 0xFF)
					}
				}
				4 -> {
					var m = 0
					for (n in 0 until header.width) {
						val r = currentRow[m++]
						val g = currentRow[m++]
						val b = currentRow[m++]
						val a = currentRow[m++]
						row[n] = RGBA.packFast(r, g, b, a)
						//System.out.printf("%02X,%02X,%02X,%02X\n", r, g, b, a)
					}
				}
				else -> noImpl("Bytes: ${header.bytes}")
			}
			out.setRow(y, row)
			val temp = currentRow
			currentRow = lastRow
			lastRow = temp
		}

		return out
	}

	fun paethPredictor(a: Int, b: Int, c: Int): Int {
		val p = a + b - c
		val pa = Math.abs(p - a)
		val pb = Math.abs(p - b)
		val pc = Math.abs(p - c)
		return if ((pa <= pb) && (pa <= pc)) a else if (pb <= pc) b else c
	}

	fun applyFilter(filter: Int, p: UByteArray, c: UByteArray, bpp: Int) {
		when (filter) {
			0 -> Unit
			1 -> for (n in bpp until c.size) c[n] += c[n - bpp]
			2 -> for (n in 0 until c.size) c[n] += p[n]
			3 -> {
				for (n in 0 until bpp) c[n] += p[n] / 2
				for (n in bpp until c.size) c[n] += (c[n - bpp] + p[n]) / 2
			}
			4 -> {
				for (n in 0 until bpp) c[n] += p[n]
				for (n in bpp until c.size) c[n] += paethPredictor(c[n - bpp], p[n], p[n - bpp])
			}
			else -> noImpl("Filter: $filter")
		}
	}
}