package com.soywiz.vitaorganizer

import com.soywiz.util.stream

class GameEntry(val gameId: String) {
	val entry = VitaOrganizerCache.entry(gameId)
	val psf by lazy {
		try {
			PSF.read(entry.paramSfoFile.readBytes().stream)
		} catch (e: Throwable) {
			mapOf<String, Any>()
		}
	}
	val hasExtendedPermissions by lazy {
		try {
			entry.permissionsFile.readText().toBoolean()
		} catch (e: Throwable) {
			true
		}
	}
	val id by lazy { psf["TITLE_ID"].toString() }
	val title by lazy { psf["TITLE"].toString() }
	var inVita = false
	var inPC = false
	val vpkFile: String? get() = entry.pathFile.readText(Charsets.UTF_8)
	val size: Long by lazy {
		try {
			entry.sizeFile.readText().toLong()
		} catch (e: Throwable) {
			0L
		}
	}

	override fun toString(): String = id
}
