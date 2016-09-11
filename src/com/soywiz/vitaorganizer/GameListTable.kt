package com.soywiz.vitaorganizer

import com.soywiz.util.stream
import com.soywiz.vitaorganizer.ext.getScaledImage
import java.awt.Font
import java.awt.Point
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

open class GameListTable : JTable(object : DefaultTableModel() {
	override fun isCellEditable(row: Int, column: Int): Boolean {
		return false
	}
}) {
	val model2 = model as DefaultTableModel

	init {
		val table = this

		table.rowHeight = 64
		table.autoCreateRowSorter = true

		//table.getColumnModel().getColumn(0).cellRenderer = JTable.IconRenderer()
		//(table.model2 as DefaultTableModel).addRow(arrayOf("John", "Doe", "Rowing", 3, true))

		table.fillsViewportHeight = true

		fun createColumn(text: String): Int {
			val id = model2.columnCount
			model2.addColumn(text.toString())
			//return table.columnModel.getColumn(id)
			return id
			//return table.getColumn(text.toString())
		}

		val ID_COLUMN_ICON = createColumn(Texts.format("COLUMN_ICON"))
		val ID_COLUMN_ID = createColumn(Texts.format("COLUMN_ID"))
		val ID_COLUMN_WHERE = createColumn(Texts.format("COLUMN_WHERE"))
		val ID_COLUMN_VERSION = createColumn(Texts.format("COLUMN_VERSION"))
		val ID_COLUMN_PERMISSIONS = createColumn(Texts.format("COLUMN_PERMISSIONS"))
		val ID_COLUMN_SIZE = createColumn(Texts.format("COLUMN_SIZE"))
		val ID_COLUMN_TITLE = createColumn(Texts.format("COLUMN_TITLE"))

		//table.autoResizeMode = JTable.AUTO_RESIZE_OFF
		table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS

		val COLUMN_ICON = table.columnModel.getColumn(ID_COLUMN_ICON)
		val COLUMN_ID = table.columnModel.getColumn(ID_COLUMN_ID)
		val COLUMN_WHERE = table.columnModel.getColumn(ID_COLUMN_WHERE)
		val COLUMN_VERSION = table.columnModel.getColumn(ID_COLUMN_VERSION)
		val COLUMN_PERMISSIONS = table.columnModel.getColumn(ID_COLUMN_PERMISSIONS)
		val COLUMN_SIZE = table.columnModel.getColumn(ID_COLUMN_SIZE)
		val COLUMN_TITLE = table.columnModel.getColumn(ID_COLUMN_TITLE)

		COLUMN_ICON.apply {
			//headerValue = Texts2.COLUMN_ICON
			width = 64
			minWidth = 64
			maxWidth = 64
			preferredWidth = 64
			resizable = false
		}
		COLUMN_ID.apply {
			width = 96
			minWidth = 96
			maxWidth = 96
			preferredWidth = 96
			resizable = false
			cellRenderer = DefaultTableCellRenderer().apply {
				horizontalAlignment = JLabel.CENTER;
			}
		}
		COLUMN_WHERE.apply {
			width = 64
			minWidth = 64
			maxWidth = 64
			preferredWidth = 64
			resizable = false
			cellRenderer = DefaultTableCellRenderer().apply {
				horizontalAlignment = JLabel.CENTER;
			}
		}
		COLUMN_VERSION.apply {
			width = 64
			minWidth = 64
			maxWidth = 64
			preferredWidth = 64
			resizable = false
			cellRenderer = DefaultTableCellRenderer().apply {
				horizontalAlignment = JLabel.CENTER;
			}
		}
		COLUMN_PERMISSIONS.apply {
			width = 96
			minWidth = 96
			maxWidth = 96
			preferredWidth = 96
			resizable = false
			cellRenderer = DefaultTableCellRenderer().apply {
				horizontalAlignment = JLabel.CENTER
			}
		}
		COLUMN_SIZE.apply {
			width = 96
			minWidth = 96
			maxWidth = 96
			preferredWidth = 96
			resizable = false
			cellRenderer = DefaultTableCellRenderer().apply {
				horizontalAlignment = JLabel.CENTER
			}
		}

		table.rowSorter = TableRowSorter<TableModel>(table.model).apply {
			setComparator(COLUMN_SIZE.modelIndex, { a, b -> (a as Comparable<Any>).compareTo((b as Comparable<Any>)) })
		}

		COLUMN_TITLE.apply {
			width = 512
			preferredWidth = 512
			//resizable = false
		}

		table.font = Font(Font.MONOSPACED, Font.PLAIN, 14)

		table.selectionModel.addListSelectionListener { e -> println(e) };

		table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
		table.columnModel.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

		table.addKeyListener(object : KeyAdapter() {
			override fun keyPressed(e: KeyEvent) {
				if (e.keyCode == KeyEvent.VK_ENTER) {
					table.showMenu()
				} else {
					super.keyPressed(e)
				}
			}
		})

		table.addMouseListener(object : MouseAdapter() {
			override fun mouseReleased(e: MouseEvent) {
				super.mouseReleased(e)
				val row = table.rowAtPoint(Point(e.x, e.y))
				table.clearSelection()
				table.addRowSelectionInterval(row, row)
				if (row >= 0) table.showMenu()
			}
		})
	}

	fun getEntryAtRow(row: Int): GameEntry = dataModel.getValueAt(this.convertRowIndexToModel(row), 1) as GameEntry

	val currentEntry: GameEntry get() = getEntryAtRow(this.selectedRow)

	fun showMenuForRow(row: Int) {
		val rect = getCellRect(row, 1, true)
		showMenuAtFor(rect.x, rect.y + rect.height, getEntryAtRow(row))
	}

	open fun showMenuAtFor(x: Int, y: Int, entry: GameEntry) {
	}

	fun showMenu() {
		showMenuForRow(selectedRow)
	}

	override fun getColumnClass(column: Int): Class<*> {
		return getValueAt(0, column).javaClass
	}

	override fun processKeyEvent(e: KeyEvent) {
		when (e.keyCode) {
			KeyEvent.VK_ENTER -> showMenu()
			else -> super.processKeyEvent(e)
		}
	}

	fun setEntries(games: List<GameEntry>) {
		val newRows = arrayListOf<Array<Any>>()

		for (entry in games.sortedBy { it.title }) {
			try {
				val gameId = entry.gameId
				val entry2 = VitaOrganizerCache.entry(gameId)
				val icon = entry2.icon0File
				val image = ImageIO.read(ByteArrayInputStream(icon.readBytes()))
				val psf = PSF.read(entry2.paramSfoFile.readBytes().stream)
				val extendedPermissions = entry.hasExtendedPermissions

				//println(psf)
				if (image != null) {
					newRows.add(arrayOf(
						ImageIcon(image.getScaledImage(64, 64)),
						entry,
						if (entry.inVita && entry.inPC) {
							Texts.format("LOCATION_BOTH")
						} else if (entry.inVita) {
							Texts.format("LOCATION_VITA")
						} else if (entry.inPC) {
							Texts.format("LOCATION_PC")
						} else {
							Texts.format("LOCATION_NONE")
						},
						psf["APP_VER"] ?: psf["VERSION"] ?: Texts.format("UNKNOWN_VERSION"),
						(if (extendedPermissions) Texts.format("PERMISSIONS_UNSECURE") else Texts.format("PERMISSIONS_SECURE")),
						FileSize(entry.size),
						entry.title
					))
				}
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}

		while (model.rowCount > 0) model2.removeRow(model.rowCount - 1)
		for (row in newRows) model2.addRow(row)

		model2.fireTableDataChanged()
	}
}