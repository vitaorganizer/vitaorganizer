package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.*
import com.soywiz.vitaorganizer.ext.safe_exists
import java.io.File

class UpdateFileListTask(vitaOrganizer: VitaOrganizer) : VitaTask(vitaOrganizer) {
	override fun perform() {
		synchronized(vitaOrganizer.VPK_GAME_FILES) {
			vitaOrganizer.VPK_GAME_FILES.clear()
		}

		if(VitaOrganizerSettings.vpkFolder.isEmpty()) {
			error("Invalid path! Please choose a valid directory!")
			return;
		}
		val fileVpkFolder = File(VitaOrganizerSettings.vpkFolder)
		if(!fileVpkFolder.safe_exists()) {
			error(Texts.format("INVALID_PATH_CHOOSE"));
			return
		}
		status(Texts.format("STEP_ANALYZING_FILES", "folder" to VitaOrganizerSettings.vpkFolder))

		val MAX_SUBDIRECTORY_LEVELS = 2

		fun listVpkFiles(folder: File, level: Int = 0): List<File> {
			val out = arrayListOf<File>()
			if (level > MAX_SUBDIRECTORY_LEVELS) return out
			for (child in folder.listFiles()) {
				if (child.isDirectory) {
					out += listVpkFiles(child, level = level + 1)
				} else {
					if (child.extension.toLowerCase() == "vpk") out += child
				}
			}
			return out
		}

		val vpkFiles = listVpkFiles(fileVpkFolder)

		for ((index, vpkFile) in vpkFiles.withIndex()) {
			val ff = VpkFile(vpkFile)
			val gameId = ff.cacheAndGetGameId()
			if (gameId != null) {
				if(gameId.length != 9) {
					//gameId has to be a length of 9 characters or it will not be installable
					//either fix gameId automatically or skip
					println("Skipped ${vpkFile.canonicalPath} because of malformed TITLE_ID: $gameId")
					VitaOrganizerCache.entry(vpkFile).delete();

					continue;
				}
				synchronized(vitaOrganizer.VPK_GAME_FILES) {
					status(Texts.format("STEP_ANALYZING_ITEM", "name" to vpkFile.name, "current" to index + 1, "total" to vpkFiles.size))
					vitaOrganizer.VPK_GAME_FILES += vpkFile
				}
			}

			//Thread.sleep(200L)
		}
		vitaOrganizer.updateEntries()
		status(Texts.format("STEP_DONE"))
	}
}
