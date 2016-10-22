package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.EbootBin
import com.soywiz.vitaorganizer.FileRules
import com.soywiz.vitaorganizer.ext.copyToReport
import com.soywiz.vitaorganizer.ext.listdirRecursively
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun createBigVpk(base: File, outputFile: File, level: Int = Deflater.DEFAULT_COMPRESSION, report: (currentSize: Long, totalSize: Long, currentFile: Int, totalFile: Int, file: String) -> Unit) {
	data class Entry(val file: File, val path: String)

	val files = base.listdirRecursively()
		.filter { !it.isDirectory }
		.map { Entry(it, it.toRelativeString(base).replace('\\', '/')) }
		.filter { FileRules.includeInBigVpk(it.path) }

	var currentFileName = ""
	var currentFile = 0
	val totalFiles = files.size
	var currentSize = 0L
	val totalSize = files.map { it.file.length() }.sum()

	fun doReport() {
		report(currentSize, totalSize, currentFile, totalFiles, currentFileName)
	}

	doReport()

	// out put file
	FileOutputStream(outputFile).use { outStream ->
		BufferedOutputStream(outStream).use { bufferedOutputStream ->
			ZipOutputStream(bufferedOutputStream).use { out ->
				out.setLevel(level)

				// name the file inside the zip  file
				for ((file, name) in files) {
					currentFileName = name
					currentFile++

					out.putNextEntry(ZipEntry(name))
					//if (e.name == "eboot.bin") {
					//	out.write(getResourceBytes("com/soywiz/vitaorganizer/dummy_eboot.bin"))
					//} else {

					if (FileRules.isEboot(name)) {
						val data = EbootBin.setSecureInplace(file.readBytes(), secure = true)
						out.write(data)
						currentSize += data.size
						doReport()
					} else {
						FileInputStream(file).use { fin ->
							fin.copyToReport(out, length = file.length()) { current, total, chunk ->
								currentSize += chunk
								doReport()
							}
						}
					}

					//}
					out.closeEntry()
				}
			}
		}
	}

	doReport()
}
