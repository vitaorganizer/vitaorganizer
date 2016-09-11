package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.Texts
import com.soywiz.vitaorganizer.VitaOrganizer
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

open class VitaTask {
	class Progress(var current: Long, var total: Long) {
		fun set(current: Long, total: Long) {
			this.current = current
			this.total = total
		}
	}

	val globalProgress = Progress(0L, 0L)
	val localProgress = Progress(0L, 0L)

	fun status(status: String) {
		SwingUtilities.invokeLater {
			VitaOrganizer.updateStatus(status)
		}
	}

	fun info(text: String) {
		SwingUtilities.invokeLater {
			JOptionPane.showMessageDialog(VitaOrganizer, text, Texts.format("INFORMATION"), JOptionPane.INFORMATION_MESSAGE)
		}
	}

	fun error(text: String) {
		SwingUtilities.invokeLater {
			JOptionPane.showMessageDialog(VitaOrganizer, text, "Error", JOptionPane.ERROR_MESSAGE)
		}
	}

	fun warn(title: String, text: String): Boolean {
		val result = JOptionPane.showConfirmDialog(VitaOrganizer, text, title, JOptionPane.YES_NO_OPTION)
		return (result == JOptionPane.YES_OPTION)
	}

	fun progress(current: Long, max: Long) {
		localProgress.set(current, max)
	}

	fun progress(current: Int, max: Int) {
		localProgress.set(current.toLong(), max.toLong())
	}

	open fun checkBeforeQueue() {
	}

	open fun perform() {
	}

	open fun cancel() {
	}
}