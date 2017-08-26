package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.*
import com.soywiz.vitaorganizer.ext.listAllFiles
import com.soywiz.vitaorganizer.ext.safe_exists
import java.io.File

class PreloadCache(vitaOrganizer: VitaOrganizer) : VitaTask(vitaOrganizer) {
	override fun perform() {
		//preload cached files
		if(VitaOrganizerCache.cacheFolder.safe_exists()) {
			val cachedPaths = VitaOrganizerCache.cacheFolder.listAllFiles("path")
			for(path in cachedPaths) {
				val file = File(path.readText())
				if(!file.safe_exists())
					continue

				synchronized(vitaOrganizer.VPK_GAME_FILES) {
					vitaOrganizer.VPK_GAME_FILES += file
				}
			}
			status("Preloading cache")
			vitaOrganizer.updateEntries()
			status("Done")
		}
	}
}
