package com.soywiz.vitaorganizer

import com.soywiz.util.Hash
import com.soywiz.util.get
import com.soywiz.util.toHexString
import com.soywiz.vitaorganizer.ext.safe_delete
import com.soywiz.vitaorganizer.ext.safe_exists
import java.io.File

object VitaOrganizerCache {
	val cacheFolder = File("vitaorganizer/cache")

	init {
		cacheFolder.mkdirs()
	}

	class Entry(val file: File) {
		val path = Hash.sha1((file.canonicalPath + "@" + file.length()).toByteArray(Charsets.UTF_8)).toHexString()
		val icon0File = cacheFolder["$path.icon0.png"]
		val paramSfoFile = cacheFolder["$path.param.sfo"]
		val pathFile = cacheFolder["$path.path"]
		val sizeFile = cacheFolder["$path.size"]
		val permissionsFile = cacheFolder["$path.extperm"]
		val dumperVersionFile = cacheFolder["$path.dumperversion"]
		val compressionFile = cacheFolder["$path.compression"]

		init {
			try {
				if (!cacheFolder.safe_exists()) {
					println("There should be a cache folder, but someone has deleted it. Recreating...")
					cacheFolder.mkdirs()
				}
			} catch (e: Throwable) {
				//this could go soooo wrong, possible in a never ending loop
				MsgMgr.error("Could not create cache directory. Trying to restart..")
				VitaOrganizer.instance.restart()
			}
		}

		fun delete() {
			icon0File.safe_delete()
			paramSfoFile.safe_delete()
			pathFile.safe_delete()
			sizeFile.safe_delete()
			permissionsFile.safe_delete()
			dumperVersionFile.safe_delete()
			compressionFile.safe_delete()
		}
	}

	fun entry(file: File) = Entry(file)

	fun deleteAll() {
		try {
			if (cacheFolder.safe_exists()) {
				cacheFolder.safe_delete();
				cacheFolder.mkdirs();
			}
		} catch (e: Throwable) {

		}
	}

	/*
	fun setIcon0File(titleId: String, data: ByteArray) {
		getIcon0File(titleId).writeBytes(data)
	}

	fun setParamSfoFile(titleId: String, data: ByteArray) {
		getParamSfoFile(titleId).writeBytes(data)
	}

	fun setVpkPath(titleId: String, path: String) {
		getVpkPathFile(titleId).writeBytes(path.toByteArray(Charsets.UTF_8))
	}

	fun getIcon0File(titleId: String): File {
		cacheFolder.mkdirs()
		return cacheFolder["$titleId.icon0.png"]
	}

	fun getParamSfoFile(titleId: String): File {
		cacheFolder.mkdirs()
		return cacheFolder["$titleId.param.sfo"]
	}

	fun getVpkPathFile(titleId: String): File {
		cacheFolder.mkdirs()
		return cacheFolder["$titleId.path"]
	}

	fun getVpkPath(titleId: String): String? {
		return try {
			getVpkPathFile(titleId).readText(Charsets.UTF_8)
		} catch (e: Throwable) {
			null
		}
	}
	*/
}
