package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.VitaOrganizer
import com.soywiz.vitaorganizer.VitaTaskQueue
import java.net.URL
import javax.swing.JOptionPane

class CheckForUpdatesTask : VitaTask() {
	override fun perform() {
		val text = URL("https://raw.githubusercontent.com/soywiz/vitaorganizer/master/lastVersion.txt").readText()
		val parts = text.lines()
		val lastVersion = parts[0]
		val lastVersionUrl = parts[1]
		if (lastVersion == VitaOrganizer.currentVersion) {
			JOptionPane.showMessageDialog(VitaOrganizer, "You have the lastest version: ${VitaOrganizer.currentVersion}", "Actions", JOptionPane.INFORMATION_MESSAGE);
		} else {
			val result = JOptionPane.showConfirmDialog(VitaOrganizer, "There is a new version: $lastVersion\nYou have: ${VitaOrganizer.currentVersion}\nWant to download last version?", "Actions", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				VitaOrganizer.openWebpage(URL(lastVersionUrl))
			}
		}
		println(parts)
	}
}