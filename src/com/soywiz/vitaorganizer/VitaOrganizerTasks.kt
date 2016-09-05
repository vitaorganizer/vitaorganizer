package com.soywiz.vitaorganizer

import java.io.File
import java.util.*

class VitaOrganizerTasks {
	private val tasks: Queue<Task> = LinkedList<Task>()

	private open class Task {
		open fun perform() {
		}
		open fun cancel() {
		}
	}

	private class UploadGame(val vpkFile: File) : Task() {

	}

	fun queueUploadGame(vpkFile: File) {
		tasks += UploadGame(vpkFile)
	}
}