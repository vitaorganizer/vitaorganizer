package com.soywiz.vitaorganizer

import com.soywiz.util.DumperNamesHelper
import com.soywiz.util.stream
import java.io.File

class CachedVpkEntry(val file: File) {
	val entry = VitaOrganizerCache.entry(file)
	val psf by lazy {
		try {
			PSF.read(entry.paramSfoFile.readBytes().stream)
		} catch (e: Throwable) {
			mapOf<String, Any>()
		}
	}
	val gameId by lazy { psf["TITLE_ID"]?.toString() ?: "UNKNOWN" }
	val hasExtendedPermissions by lazy {
		try {
			entry.permissionsFile.readText().toBoolean()
		} catch (e: Throwable) {
			true
		}
	}
	val attribute by lazy { psf["ATTRIBUTE"].toString() }
	val id by lazy { psf["TITLE_ID"].toString() }
	val title by lazy { psf["TITLE"].toString() }
	val dumperVersion by lazy {
		val text = if (attribute == "32768")
			"HB"
		else if (entry.dumperVersionFile.exists())
			entry.dumperVersionFile.readText();
		else
			"UNKNOWN"

		DumperNamesHelper().findDumperByShortName(text).longName
	}
	val compressionLevel by lazy {
		if (entry.compressionFile.exists()) {
			//see https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT
			//4.4.5
			val method = entry.compressionFile.readText()
			when (method) {
				"0" -> "not compressed"
				"1" -> "shrunk"
				"2" -> "compression factor 1"
				"3" -> "compression factor 2"
				"4" -> "compresison factor 3"
				"5" -> "compression factor 4"
				"6" -> "imploded"
				"7" -> "reversed"
				"8" -> "deflate"
				"9" -> "deflate64"
				else -> method
			}
		} else
			"could not read from param.sfo"
	}
	//var inVita = false
	//var inPC = false
	val vpkLocalPath: String? get() = entry.pathFile.readText(Charsets.UTF_8)
	val vpkLocalFile: File? get() = if (vpkLocalPath != null) File(vpkLocalPath) else null
	val vpkLocalVpkFile: VpkFile? get() = if (vpkLocalPath != null) VpkFile(File(vpkLocalPath)) else null
	val size: Long by lazy {
		try {
			entry.sizeFile.readText().toLong()
		} catch (e: Throwable) {
			0L
		}
	}

	override fun toString(): String = id
}
