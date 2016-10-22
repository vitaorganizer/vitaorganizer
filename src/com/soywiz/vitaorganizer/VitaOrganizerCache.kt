package com.soywiz.vitaorganizer

import com.soywiz.util.Hash
import com.soywiz.util.get
import com.soywiz.util.toHexString
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

        fun delete() {
            icon0File.delete()
            paramSfoFile.delete()
            pathFile.delete()
            sizeFile.delete()
            permissionsFile.delete()
            dumperVersionFile.delete()
            compressionFile.delete()
        }
    }

    fun entry(file: File) = Entry(file)

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