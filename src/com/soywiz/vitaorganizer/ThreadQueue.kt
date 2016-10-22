package com.soywiz.vitaorganizer

import java.util.*

class ThreadQueue {
	var running = false; private set
	private val tasks = LinkedList<Task>()

	val hasPendingTasks: Boolean get() = running || synchronized(tasks) { tasks.isNotEmpty() }

	private fun readTaskAndMarkRunning() = synchronized(tasks) {
		val now = System.currentTimeMillis()
		val item = tasks.firstOrNull { now >= it.timestamp }
		if (item != null) {
			tasks.remove(item)
			item
		} else {
			null
		}
	}

	val thread = Thread {
		while (VitaOrganizer.instance.isVisible) {
			Thread.sleep(10L)
			val task = readTaskAndMarkRunning()
			if (task != null) {
				try {
					task.action()
				} catch (t: Throwable) {
					t.printStackTrace()
				} finally {
					running = false
				}
			}
			running = false
		}
	}.apply {
		isDaemon = true
		start()
	}

	fun waitCompleted() {
		while (hasPendingTasks) Thread.sleep(10L)
	}

	fun clear() {
		synchronized(tasks) {
			tasks.clear()
		}
	}

	fun queueAfter(time: Int, task: () -> Unit) {
		try {
			synchronized(tasks) {
				tasks += Task(task, System.currentTimeMillis() + time)
			}
		} catch (t: Throwable) {
			t.printStackTrace()
		}
	}

	fun queue(task: () -> Unit) {
		queueAfter(0, task)
	}

	class Task(val action: () -> Unit, val timestamp: Long)
}