package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.*
import java.util.zip.ZipFile
import javax.swing.JOptionPane

class SendPromotingVpkToVitaTask(vitaOrganizer: VitaOrganizer, val vpkFile: VpkFile) : VitaTask(vitaOrganizer) {
	val vpkPath = "ux0:/organizer/${vpkFile.id}.VPK"

	override fun checkBeforeQueue() {
		//updateStatus(Texts.format("STEP_CHECKING_EBOOT_PERMISSIONS"))
		if (vpkFile.hasExtendedPermissions) {
			if (!warn(
				Texts.format("WARNING_EX"),
				Texts.format("WARNING_UNSAFE", "id" to vpkFile.id)
			)) {
				throw InterruptedException(Texts.format("UNSAFE_NOT_ACCEPTED"))
			}
		}
	}

	fun performBase() {
		status(Texts.format("STEP_GENERATING_SMALL_VPK_FOR_PROMOTING"))

		//val zip = ZipFile(entry.vpkFile)
		try {
			val vpkData = ZipFile(vpkFile.vpkFile).use { zip -> createSmallVpk(zip) }

			status(Texts.format("STEP_GENERATED_SMALL_VPK_FOR_PROMOTING"))

			PsvitaDevice.uploadFile("/$vpkPath", vpkData) { status ->
				progress(status.currentSize, status.totalSize)
				status(Texts.format("STEP_UPLOADING_VPK_FOR_PROMOTING", "current" to status.currentSizeString, "total" to status.totalSizeString, "speed" to status.speedString))
			}
		} catch (e: Throwable) {
			e.printStackTrace()
			JOptionPane.showMessageDialog(vitaOrganizer, "${e.toString()}", "${e.message}", JOptionPane.ERROR_MESSAGE);
		}
	}

	override fun perform() {
		performBase()
		status(Texts.format("GAME_SENT_SUCCESSFULLY", "id" to vpkFile.id))
		info(Texts.format("VITASHELL_INSTALL", "vpkPath" to vpkPath))
	}
}