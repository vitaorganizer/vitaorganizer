package com.soywiz.vitaorganizer.tasks

import com.soywiz.util.get
import com.soywiz.util.invalidOp
import com.soywiz.vitaorganizer.FileSize
import com.soywiz.vitaorganizer.VitaOrganizer
import java.io.File
import java.util.zip.Deflater

class CreateVpkFromFolderVitaTask(vitaOrganizer: VitaOrganizer, val folder: File, val outVpk: File, val level: Int = Deflater.BEST_COMPRESSION) : VitaTask(vitaOrganizer) {
	override fun checkBeforeQueue() {
		if (!folder["eboot.bin"].exists()) invalidOp("eboot.bin doesn't exists")
		if (!folder["sce_sys/param.sfo"].exists()) invalidOp("sce_sys/param.sfo doesn't exists")
	}

	override fun perform() {
		status("CreateVpkFromFolderVitaTask started")
		createBigVpk(folder, outVpk, level = level) { currentSize, totalSize, currentFile, totalFile, file ->
			status("CreateVpkFromFolderVitaTask: $file : $currentFile/$totalFile - ${FileSize.toString(currentSize)}/${FileSize.toString(totalSize)}")
		}
		status("CreateVpkFromFolderVitaTask done")
	}
}
