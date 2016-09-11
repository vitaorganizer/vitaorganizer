package com.soywiz.vitaorganizer.tasks

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

fun createSmallVpk(zip: ZipFile): ByteArray {
	// out put file
	val outBytes = ByteArrayOutputStream()

	ZipOutputStream(outBytes).use { out ->
		out.setLevel(Deflater.DEFAULT_COMPRESSION)

		// name the file inside the zip  file
		for (e in zip.entries()) {
			if (e.name == "eboot.bin" || e.name.startsWith("sce_sys/")) {
				out.putNextEntry(ZipEntry(e.name))
				out.write(zip.getInputStream(e).readBytes())
				out.closeEntry()
			}
		}
	}

	return outBytes.toByteArray()
}
