package com.soywiz.vitaorganizer

import util.get
import java.io.File

object VitaOrganizerCache {
    val cacheFolder = File("vitaorganizer/cache")

    fun getIcon0File(titleId: String): File {
        cacheFolder.mkdirs()
        return cacheFolder["$titleId.icon0.png"]
    }

    fun getParamSfoFile(titleId: String): File {
        cacheFolder.mkdirs()
        return cacheFolder["$titleId.param.sfo"]
    }
}