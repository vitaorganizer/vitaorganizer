package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.*
import java.util.zip.ZipFile
import javax.swing.JOptionPane

class SendPromotingVpkToVitaTask(val entry: GameEntry) : VitaTask() {
	val zip = ZipFile(entry.vpkFile)
	val vpkPath = "ux0:/organizer/${entry.id}.VPK"

	override fun checkBeforeQueue() {
		//updateStatus(Texts.format("STEP_CHECKING_EBOOT_PERMISSIONS"))
		if (entry.hasExtendedPermissions) {
			if (!warn("WARNING!", "Game ${entry.id} requires extended permissions.\nAre you sure you want to install it. It could damage your device?")) {
				throw InterruptedException("Not accepted installing game with extended permissions")
			}
		}
	}

	override fun perform() {
		updateStatus(Texts.format("STEP_GENERATING_SMALL_VPK_FOR_PROMOTING"))

		//val zip = ZipFile(entry.vpkFile)
		try {
			val vpkData = createSmallVpk(zip)

			updateStatus(Texts.format("STEP_GENERATED_SMALL_VPK_FOR_PROMOTING"))

			PsvitaDevice.uploadFile("/$vpkPath", vpkData) { status ->
				progress(status.currentSize, status.totalSize)
				updateStatus(Texts.format("STEP_UPLOADING_VPK_FOR_PROMOTING", "current" to status.currentSize, "total" to status.totalSize))
			}
		} catch (e: Throwable) {
			e.printStackTrace()
			JOptionPane.showMessageDialog(VitaOrganizer, "${e.toString()}", "${e.message}", JOptionPane.ERROR_MESSAGE);
		}
		updateStatus("Sent game vpk ${entry.id}")
		info("Now use VitaShell to install\n$vpkPath\n\nAfter that active ftp again and use this program to Send Data to PSVita")

		zip.close()
	}
}