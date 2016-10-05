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
	val attribute by lazy { psf["ATTRIBUTE"].toString() }
	val id by lazy { psf["TITLE_ID"].toString() }
	val title by lazy { psf["TITLE"].toString() }
	val dumperVersion by lazy { 
        var text = "UNKNOWN";
		if(attribute == "32768")
			text = "HB"
    	else if(entry.dumperVersionFile.exists())
			text = entry.dumperVersionFile.readText(); 

        DumperNamesHelper().findDumperByShortName(text).longName 
    }
	val compressionLevel by lazy { 
        if(entry.compressionFile.exists())  {
			//see https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT
			//4.4.5
			val method = entry.compressionFile.readText() 
			if(method == "0")
				"not compressed"
			else if(method == "1")
				"shrunk"
			else if(method == "2")
				"compresion factor 1"
			else if(method == "3")
				"compresion factor 2"
			else if(method == "4")
				"compresion factor 3"
			else if(method == "5")
				"compresion factor 4"
			else if(method == "6")
				"imploded"
			else if(method == "7")
				"reversed"
			else if(method == "8")
				"deflate"
			else if(method == "9")
				"shrunk64"
			else
				method
		}
        else 
			"could not read from param.sfo" 
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
