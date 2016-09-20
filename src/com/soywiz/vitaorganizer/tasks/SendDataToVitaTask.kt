package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.*
import java.util.zip.ZipFile

class SendDataToVitaTask(vitaOrganizer: VitaOrganizer, val vpkFile: VpkFile) : VitaTask(vitaOrganizer) {
	fun performBase() {

		status(Texts.format("STEP_SENDING_GAME", "id" to vpkFile.id))
		//val zip = ZipFile(entry.vpkFile)
		try {
			ZipFile(vpkFile.vpkFile).use { zip ->
				PsvitaDevice.uploadGame(vpkFile.id, zip, filter = { path ->
					// Skip files already installed in the VPK
					if (path == "eboot.bin" || path.startsWith("sce_sys/")) {
						false
					} else {
						true
					}
				}) { status ->
					//println("$status")
					Texts.format("STEP_SENDING_GAME_UPLOADING", "id" to vpkFile.id, "fileRange" to status.fileRange, "sizeRange" to status.sizeRange, "speed" to status.speedString)
					status(Texts.format("STEP_UPLOADING_VPK_FOR_PROMOTING", "current" to status.currentSizeString, "total" to status.totalSizeString, "speed" to status.speedString))
				}
			}
			//statusLabel.text = "Processing game ${vitaGameCount + 1}/${vitaGameIds.size} ($gameId)..."
		} catch (e: Throwable) {
			error(e.toString())
		}
		status(Texts.format("SENT_GAME_DATA", "id" to vpkFile.id))
	}

	override fun perform() {
		performBase()
		status(Texts.format("GAME_SENT_SUCCESSFULLY", "id" to vpkFile.id))
	}
}