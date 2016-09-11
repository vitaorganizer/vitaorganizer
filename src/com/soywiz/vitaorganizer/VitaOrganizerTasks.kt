package com.soywiz.vitaorganizer

import java.util.*
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

class VitaOrganizerTasks {
	private val tasks: Queue<Task> = LinkedList<Task>()
	val thread = Thread {
		while (true) {
			Thread.sleep(10L)
			val task = synchronized(tasks) { if (tasks.isNotEmpty()) tasks.remove() else null }
			if (task != null) {
				try {
					task.perform()
				} catch (t: Throwable) {
					t.printStackTrace()
				}
			}
		}
	}.apply {
		isDaemon = true
		start()
	}

	open class Task {
		fun updateStatus(status: String) {
			SwingUtilities.invokeLater {
				VitaOrganizer.instance.updateStatus(status)
			}
		}

		fun info(text: String) {
			SwingUtilities.invokeLater {
				JOptionPane.showMessageDialog(VitaOrganizer.instance, text, Texts.format("INFORMATION"), JOptionPane.INFORMATION_MESSAGE)
			}
		}

		fun error(text: String) {
			SwingUtilities.invokeLater {
				JOptionPane.showMessageDialog(VitaOrganizer.instance, text, "Error", JOptionPane.ERROR_MESSAGE)
			}
		}

		fun warn(title: String, text: String): Boolean {
			val result = JOptionPane.showConfirmDialog(VitaOrganizer.instance, text, title, JOptionPane.YES_NO_OPTION)
			return (result == JOptionPane.YES_OPTION)
		}

		fun progress(min: Long, max: Long) {
		}

		open fun checkBeforeQueue() {
		}

		open fun perform() {
		}

		open fun cancel() {
		}
	}

	fun queueTask(task: Task) {
		try {
			task.checkBeforeQueue()
			synchronized(tasks) {
				tasks += task
			}
		} catch (t: Throwable) {
			t.printStackTrace()
		}
	}
}