package com.soywiz.vitaorganizer

import com.soywiz.util.DumperModules
import com.soywiz.util.DumperNames
import com.soywiz.util.DumperNamesHelper
import com.soywiz.util.open2
import com.soywiz.vitaorganizer.ext.getBytes
import com.soywiz.vitaorganizer.ext.getResourceBytes
import java.io.File
import java.util.zip.ZipFile

class VpkFile(val vpkFile: File) {
	//val entry: GameEntry by lazy { VpkFile() }

	val paramSfoData: ByteArray by lazy {
		try {
			ZipFile(vpkFile).use { zip ->
				zip.getBytes("sce_sys/param.sfo")
			}
		} catch (e: Throwable) {
			byteArrayOf()
		}
	}

	val psf by lazy {
		try {
			PSF.read(paramSfoData.open2("r"))
		} catch (e: Throwable) {
			hashMapOf<String, Any>()
		}
	}

	val id by lazy { psf["TITLE_ID"].toString() }
	val title by lazy { psf["TITLE"].toString() }
	val hasExtendedPermissions: Boolean by lazy {
		try {
			ZipFile(vpkFile).use { zip ->
				val ebootBinData = zip.getBytes("eboot.bin")
				EbootBin.hasExtendedPermissions(ebootBinData.open2("r"))
			}
		} catch (e: Throwable) {
			true
		}
	}

	fun cacheAndGetGameId(): String? {
		var retGameId:String? = null
		try {
			ZipFile(vpkFile).use { zip ->
				val psf = psf
				val zipEntries = zip.entries()
				val gameId = psf["TITLE_ID"].toString()
				retGameId = gameId

				val entry = VitaOrganizerCache.entry(vpkFile)

				//try to find compressionlevel and vitaminversion or maiversion
				val paramsfo = zip.getEntry("sce_sys/param.sfo")
				val compressionLevel = if (paramsfo != null) paramsfo.method.toString() else ""

				var dumper = DumperNamesHelper().findDumperByShortName(if(psf["ATTRIBUTE"].toString() == "32768") "HB" else "UNKNOWN")
				if(dumper == DumperNames.UNKNOWN) {
					for (file in DumperModules.values()) {
						val suprx = zip.getEntry(file.file)
						if (suprx != null) {
							dumper = DumperNamesHelper().findDumperBySize(suprx.size)
						}
					}
				}

				println("For file [${vpkFile}] (Compressmethod : $compressionLevel Dumpver : ${dumper})")
				entry.pathFile.writeBytes(vpkFile.absolutePath.toByteArray(Charsets.UTF_8))
				if (!entry.compressionFile.exists()) {
					entry.compressionFile.writeText(compressionLevel.toString())
				}
				if (!entry.dumperVersionFile.exists()) {
					entry.dumperVersionFile.writeText(dumper.shortName)
				}

				if (!entry.icon0File.exists()) {
					try {
						entry.icon0File.writeBytes(zip.getInputStream(zip.getEntry("sce_sys/icon0.png")).readBytes())
					} catch (e: Throwable){
						entry.icon0File.writeBytes(getResourceBytes("com/soywiz/vitaorganizer/dummy128.png") ?: byteArrayOf())
					}
				}
				if (!entry.paramSfoFile.exists()) {
					entry.paramSfoFile.writeBytes(paramSfoData)
				}
				if (!entry.sizeFile.exists()) {
					val uncompressedSize = zipEntries.toList().map { it.size }.sum()
					entry.sizeFile.writeText("" + uncompressedSize)
				}
				if (!entry.permissionsFile.exists()) {
					val ebootBinData = zip.getBytes("eboot.bin")
					entry.permissionsFile.writeText("" + EbootBin.hasExtendedPermissions(ebootBinData.open2("r")))
				}
				//getGameEntryById(gameId).inPC = true
			}
		} catch (e: Throwable) {
			println("Error processing ${vpkFile.name}")
			e.printStackTrace()
		}
		return retGameId
	}


}