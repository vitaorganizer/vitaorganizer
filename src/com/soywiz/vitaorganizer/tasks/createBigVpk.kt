package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.FileRules
import com.soywiz.vitaorganizer.ext.listdirRecursively
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun createBigVpk(base: File): ByteArray {
	val files = base.listdirRecursively()
	// out put file
	val outBytes = ByteArrayOutputStream()

	ZipOutputStream(outBytes).use { out ->
		out.setLevel(Deflater.DEFAULT_COMPRESSION)

		// name the file inside the zip  file
		for (file in files) {
			if (file.isDirectory) continue
			val name = file.toRelativeString(base)
			if (FileRules.includeInBigVpk(name)) {
				out.putNextEntry(ZipEntry(name))
				//if (e.name == "eboot.bin") {
				//	out.write(getResourceBytes("com/soywiz/vitaorganizer/dummy_eboot.bin"))
				//} else {
				FileInputStream(file).use { fin ->
					fin.copyTo(out, 0x10000)
				}
				//}
				out.closeEntry()
			}
		}
	}

	return outBytes.toByteArray()
}
