package com.soywiz.vitaorganizer

import com.soywiz.util.OS
import java.io.File

object VitaOrganizerFolders {
	private val insideMacApp get() = File(".").canonicalPath.contains(".app/")

	val CONFIG_ROOT: File = when {
		OS.isMac && insideMacApp -> File(System.getenv("HOME") + "/Library/Application Support/vitaorganizer-1") // Format
		else -> File("./vitaorganizer").canonicalFile
	}.canonicalFile

	init {
		println("CONFIG_ROOT: ${CONFIG_ROOT.canonicalPath}")
		CONFIG_ROOT.mkdirs()
	}
}