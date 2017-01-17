package com.soywiz.vitaorganizer

import com.soywiz.util.DumperModules
import com.soywiz.util.DumperNames
import com.soywiz.util.DumperNamesHelper
import com.soywiz.util.open2
import com.soywiz.vitaorganizer.ext.getBytes
import com.soywiz.vitaorganizer.ext.getInputStream
import com.soywiz.vitaorganizer.ext.getResourceBytes
import com.soywiz.vitaorganizer.ext.safe_exists
import java.io.File
import java.util.zip.ZipException
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
				val stream = zip.getInputStream("eboot.bin")
				val ret = EbootBin.hasExtendedPermissions(stream)
				stream.close()
				ret
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
				val compressionLevel = paramsfo?.method?.toString() ?: ""

				var dumper = DumperNamesHelper().findDumperByShortName(if(psf["ATTRIBUTE"].toString().toInt() == 0x8000) "HB" else "UNKNOWN")
				if(dumper == DumperNames.UNKNOWN) {
					for (file in DumperModules.values()) {
						val suprx = zip.getEntry(file.file)
						if (suprx != null) {
							dumper = DumperNamesHelper().findDumperBySize(suprx.size)
						}
					}
				}

				println("Processing [$vpkFile]")
				entry.pathFile.writeBytes(vpkFile.absolutePath.toByteArray(Charsets.UTF_8))
				if (!entry.compressionFile.safe_exists()) {
					entry.compressionFile.writeText(compressionLevel.toString())
				}
				if (!entry.dumperVersionFile.safe_exists()) {
					entry.dumperVersionFile.writeText(dumper.shortName)
				}

				if (!entry.icon0File.safe_exists()) {
					if(zip.getEntry("sce_sys/icon0.png") != null)
						entry.icon0File.writeBytes(zip.getBytes("sce_sys/icon0.png"))
					else
						entry.icon0File.writeBytes(byteArrayOf())
				}
				if (!entry.paramSfoFile.safe_exists()) {
					entry.paramSfoFile.writeBytes(paramSfoData)
				}
				if (!entry.sizeFile.safe_exists()) {
					val uncompressedSize = zipEntries.toList().map { it.size }.sum()
					entry.sizeFile.writeText("" + uncompressedSize)
				}
				if (!entry.permissionsFile.safe_exists()) {
					val ebootBinStream = zip.getInputStream("eboot.bin")
					entry.permissionsFile.writeText("" + EbootBin.hasExtendedPermissions(ebootBinStream))
					ebootBinStream.close()
				}
				//getGameEntryById(gameId).inPC = true
			}
		}
		catch (e: ZipException) {
			if(e.message!!.contains("error in opening zip file"))
				println("Skipped: Could not open ${vpkFile.name}")
			else if(e.message!!.contains("invalid LOC header (bad signature)"))
				println("Skipped: Invalid LOC header in ${vpkFile.name}")
			else if(e.message!!.contains("invalid CEN header (bad signature)"))
				println("Skipped: Invalid CEN header in ${vpkFile.name}")
		}
		catch (e: Throwable) {
			println("Skipped: Error processing ${vpkFile.name}")
			e.printStackTrace()
		}
		return retGameId
	}


}
