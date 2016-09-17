package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.GameEntry
import com.soywiz.vitaorganizer.PsvitaDevice
import com.soywiz.vitaorganizer.Texts
import com.soywiz.vitaorganizer.VitaOrganizer
import java.util.zip.ZipFile
import javax.swing.JOptionPane

class SendPromotingVpkToVitaTask(val entry: GameEntry) : VitaTask() {
	val vpkPath = "ux0:/organizer/${entry.id}.VPK"

	override fun checkBeforeQueue() {
		//updateStatus(Texts.format("STEP_CHECKING_EBOOT_PERMISSIONS"))
		if (entry.hasExtendedPermissions) {
			if (!warn(
				Texts.format("WARNING_EX"),
				Texts.format("WARNING_UNSAFE", "id" to entry.id)
			)) {
				throw InterruptedException(Texts.format("UNSAFE_NOT_ACCEPTED"))
			}
		}
	}

	fun performBase() {
		status(Texts.format("STEP_GENERATING_SMALL_VPK_FOR_PROMOTING"))

		//val zip = ZipFile(entry.vpkFile)
		try {
			val vpkData = ZipFile(entry.vpkLocalPath).use { zip -> createSmallVpk(zip) }

			status(Texts.format("STEP_GENERATED_SMALL_VPK_FOR_PROMOTING"))

			PsvitaDevice.uploadFile("/$vpkPath", vpkData) { status ->
				progress(status.currentSize, status.totalSize)
				status(Texts.format("STEP_UPLOADING_VPK_FOR_PROMOTING", "current" to status.currentSize, "total" to status.totalSize))
			}
		} catch (e: Throwable) {
			e.printStackTrace()
			JOptionPane.showMessageDialog(VitaOrganizer, "${e.toString()}", "${e.message}", JOptionPane.ERROR_MESSAGE);
		}
	}

	override fun perform() {
		performBase()
		status(Texts.format("GAME_SENT_SUCCESSFULLY", "id" to entry.id))
		info(Texts.format("VITASHELL_INSTALL", "vpkPath" to vpkPath))
	}
}