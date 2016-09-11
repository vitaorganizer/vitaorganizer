package com.soywiz.vitaorganizer.popups

import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class KeyValueViewerFrame(title: String, val map: Map<String, Any>) : JFrame(title) {
	init {
		add(JPanel(BorderLayout()).apply {
			setSize(640, 480)
			val pairs = map.entries.toList()
			//val keys = pairs.map { it.key }
			//val values = pairs.map { it.value }

			val arrayPairs = pairs.map { arrayOf(it.key, it.value) }.toTypedArray()

			val model = DefaultTableModel()
			val table = JTable(model)
			model.addColumn("Key")
			model.addColumn("Value")
			for (pair in arrayPairs) model.addRow(pair)

			table.columnModel.getColumn(0).apply {
				preferredWidth = 160
				minWidth = 160
			}
			table.columnModel.getColumn(1).apply {
				preferredWidth = 1000
			}
			table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS

			add(JScrollPane(table.apply {
				autoCreateRowSorter = true
				fillsViewportHeight = true
				font = Font(Font.MONOSPACED, Font.PLAIN, 14)
			}).apply {
				setSize(640, 480)
			}, BorderLayout.CENTER)
		})
	}
}