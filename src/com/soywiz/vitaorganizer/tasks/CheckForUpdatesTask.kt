package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.Texts
import com.soywiz.vitaorganizer.VitaOrganizer
import com.soywiz.vitaorganizer.ext.openWebpage
import java.net.URL
import javax.swing.JOptionPane

class CheckForUpdatesTask : VitaTask() {
	override fun perform() {
		val text = URL("https://raw.githubusercontent.com/soywiz/vitaorganizer/master/lastVersion.txt").readText()
		val parts = text.lines()
		val lastVersion = parts[0]
		val lastVersionUrl = parts[1]
		if (lastVersion == VitaOrganizer.currentVersion) {
			JOptionPane.showMessageDialog(
				VitaOrganizer,
				Texts.format("YOU_HAVE_LASTEST_VERSION", "version" to VitaOrganizer.currentVersion),
				Texts.format("ACTIONS_TITLE"),
				JOptionPane.INFORMATION_MESSAGE
			);
		} else {
			val result = JOptionPane.showConfirmDialog(
				VitaOrganizer,
				Texts.format("NEW_VERSION_AVAILABLE", "lastVersion" to lastVersion, "currentVersion" to VitaOrganizer.currentVersion),
				Texts.format("ACTIONS_TITLE"),
				JOptionPane.YES_NO_OPTION
			);
			if (result == JOptionPane.OK_OPTION) {
				openWebpage(URL(lastVersionUrl))
			}
		}
		println(parts)
	}
}