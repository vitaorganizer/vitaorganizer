package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.GameEntry
import com.soywiz.vitaorganizer.PsvitaDevice
import java.util.zip.ZipFile

class SendDataToVitaTask(val entry: GameEntry) : VitaTask() {
	fun performBase() {
		status("Sending game ${entry.id}...")
		//val zip = ZipFile(entry.vpkFile)
		try {
			ZipFile(entry.vpkLocalPath).use { zip ->
				PsvitaDevice.uploadGame(entry.id, zip, filter = { path ->
					// Skip files already installed in the VPK
					if (path == "eboot.bin" || path.startsWith("sce_sys/")) {
						false
					} else {
						true
					}
				}) { status ->
					//println("$status")
					status("Uploading ${entry.id} :: ${status.fileRange} :: ${status.sizeRange}")
				}
			}
			//statusLabel.text = "Processing game ${vitaGameCount + 1}/${vitaGameIds.size} ($gameId)..."
		} catch (e: Throwable) {
			error(e.toString())
		}
		status("Sent game data ${entry.id}")
	}

	override fun perform() {
		performBase()
		info("Game ${entry.id} sent successfully")
	}
}