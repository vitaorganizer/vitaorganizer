package com.soywiz.vitaorganizer

import com.soywiz.util.stream
import com.soywiz.util.DumperNamesHelper
import java.io.File

class CachedGameEntry(val gameId: String) {
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
	val dumperVersion by lazy { 
        var text = "UNKNOWN";
        if (entry.dumperVersionFile.exists())
          text = entry.dumperVersionFile.readText();  
        DumperNamesHelper().findDumperByShortName(text).longName 
    }
	val compressionLevel by lazy { 
        if(entry.compressionFile.exists()) 
            entry.compressionFile.readText() 
        else 
            "0" 
    }
	var inVita = false
	var inPC = false
	val vpkLocalPath: String? get() = entry.pathFile.readText(Charsets.UTF_8)
	val vpkLocalFile: File? get() = if (vpkLocalPath != null) File(vpkLocalPath!!) else null
	val vpkLocalVpkFile: VpkFile? get() = if (vpkLocalPath != null) VpkFile(File(vpkLocalPath!!)) else null
	val size: Long by lazy {
		try {
			entry.sizeFile.readText().toLong()
		} catch (e: Throwable) {
			0L
		}
	}

	override fun toString(): String = id
}
