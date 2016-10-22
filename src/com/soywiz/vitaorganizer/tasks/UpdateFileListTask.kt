package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.Texts
import com.soywiz.vitaorganizer.VitaOrganizer
import com.soywiz.vitaorganizer.VitaOrganizerSettings
import com.soywiz.vitaorganizer.VpkFile
import java.io.File

class UpdateFileListTask(vitaOrganizer: VitaOrganizer) : VitaTask(vitaOrganizer) {
	override fun perform() {
		synchronized(vitaOrganizer.VPK_GAME_FILES) {
			vitaOrganizer.VPK_GAME_FILES.clear()
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

		val vpkFiles = listVpkFiles(File(VitaOrganizerSettings.vpkFolder))

		for ((index, vpkFile) in vpkFiles.withIndex()) {
			val ff = VpkFile(vpkFile)
			val gameId = ff.cacheAndGetGameId()
			if (gameId != null) {
				synchronized(vitaOrganizer.VPK_GAME_FILES) {
					status(Texts.format("STEP_ANALYZING_ITEM", "name" to gameId, "current" to index + 1, "total" to vpkFiles.size))
					vitaOrganizer.VPK_GAME_FILES += vpkFile
				}
			}

			//Thread.sleep(200L)
		}
		status(Texts.format("STEP_DONE"))
		vitaOrganizer.updateEntries()
	}
}