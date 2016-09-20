package com.soywiz.vitaorganizer.tasks

import com.soywiz.util.DumperModules
import com.soywiz.util.DumperNames
import com.soywiz.util.DumperNamesHelper
import com.soywiz.util.open2
import com.soywiz.vitaorganizer.*
import com.soywiz.vitaorganizer.ext.getBytes
import java.io.File
import java.util.zip.ZipFile

class UpdateFileListTask(vitaOrganizer: VitaOrganizer) : VitaTask(vitaOrganizer) {
	override fun perform() {
		synchronized(vitaOrganizer.VPK_GAME_IDS) {
			vitaOrganizer.VPK_GAME_IDS.clear()
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

		for (vpkFile in vpkFiles) {
			val ff = VpkFile(vpkFile)
			val gameId = ff.cacheAndGetGameId()
			if (gameId != null) {
				synchronized(vitaOrganizer.VPK_GAME_IDS) {
					vitaOrganizer.VPK_GAME_IDS += gameId
				}
			}

			//Thread.sleep(200L)
		}
		status(Texts.format("STEP_DONE"))
		vitaOrganizer.updateEntries()
	}
}