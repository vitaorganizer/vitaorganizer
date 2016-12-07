package com.soywiz.vitaorganizer

import com.soywiz.util.stream
import com.soywiz.vitaorganizer.ext.getScaledImage
import java.awt.BorderLayout
import java.awt.Font
import java.awt.Point
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.RowSorterEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

open class GameListTable : JPanel(BorderLayout()) {
	val model2 = object : DefaultTableModel() {
		override fun isCellEditable(row: Int, column: Int): Boolean {
			return false
		}
	}


	val table = object : JTable(model2) {
		override fun getColumnClass(column: Int): Class<*> {
			return getValueAt(0, column).javaClass
		}
	}
	val scrollPanel = JScrollPane(table)
	lateinit var sorter: TableRowSorter<TableModel>

	var filter = ""
		set(value) {
			field = value
			updateRowFilter(value)
		}
		get() = field

	private fun updateRowFilter(value: String) {
		if (value.isNullOrEmpty()) {
			sorter.rowFilter = null
		} else {
			val matcher = Regex("(?i)$value")

			sorter.rowFilter = RowFilter.regexFilter("(?i)$value")
			sorter.rowFilter = object : RowFilter<TableModel, Any>() {
				override fun include(value: Entry<out TableModel, out Any>): Boolean {
					//matcher.matches(entry.getStringValue(entry.))
					//val model = entry.model
					//table.cell
					//return true

					for (index in 0 until value.valueCount) {
						if (index < value.valueCount) {
							if (include(value.getValue(index))) return true
						}
					}
					return false
				}

				private fun include(item: Any): Boolean {
					when (item) {
						is CachedVpkEntry -> {
							for (value in item.psf.values) {
								if (matcher.containsMatchIn(value.toString())) return true
							}
							return false
						}
						else -> return matcher.containsMatchIn(item.toString())
					}
				}
			}
		}
	}

	fun setSorter() {
		sorter = TableRowSorter<TableModel>(model2).apply {
			setComparator(5, { a, b -> (a as Comparable<Any>).compareTo((b as Comparable<Any>)) })
			//rowFilter = object : RowFilter<TableModel, Any>() {
			//	override fun include(entry: Entry<out TableModel, out Any>?): Boolean {
			//		return true
			//	}
			//}
		}
		sorter.addRowSorterListener { e ->
			if (e.type === RowSorterEvent.Type.SORTED) {
				// We need to call both revalidate() and repaint()
				table.revalidate()
				table.repaint()
			}
		}
		table.rowSorter = sorter
	}

	//val sorter = (rowSorter as DefaultRowSorter<DefaultTableModel, Any?>)

	init {
		add(scrollPanel, BorderLayout.CENTER)

		table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "none")

		//table.rowSorter

		//table.rowSorter = sorter


		//sorter.rowFilter = RowFilter.regexFilter(".*foo.*")
		//sorter.sortsOnUpdates = true


		table.rowHeight = 64

		//JTable table = new JTable(myModel);
		//table.setRowSorter(sorter);

		//table.autoCreateRowSorter = true

		//rowSorter.rowFilter = RowFilter.regexFilter<TableModel, Any>("Plant")
		/*
		rowSorter.rowFilter = object : RowFilter<TableModel, Any>() {
			override fun include(entry: Entry<out TableModel, out Any>?): Boolean {
				return false
			}
		}
		*/

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
		//val ID_COLUMN_WHERE = createColumn(Texts.format("COLUMN_WHERE"))
		val ID_COLUMN_TYPE = createColumn(Texts.format("COLUMN_TYPE"))
		val ID_COLUMN_VERSION = createColumn(Texts.format("COLUMN_VERSION"))
		val ID_COLUMN_PERMISSIONS = createColumn(Texts.format("COLUMN_PERMISSIONS"))
		val ID_COLUMN_SIZE = createColumn(Texts.format("COLUMN_SIZE"))
		val ID_COLUMN_TITLE = createColumn(Texts.format("COLUMN_TITLE"))

		//table.autoResizeMode = JTable.AUTO_RESIZE_OFF
		table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS

		val COLUMN_ICON = table.columnModel.getColumn(ID_COLUMN_ICON)
		val COLUMN_ID = table.columnModel.getColumn(ID_COLUMN_ID)
		val COLUMN_TYPE = table.columnModel.getColumn(ID_COLUMN_TYPE)
		//val COLUMN_WHERE = table.columnModel.getColumn(ID_COLUMN_WHERE)
		val COLUMN_VERSION = table.columnModel.getColumn(ID_COLUMN_VERSION)
		val COLUMN_PERMISSIONS = table.columnModel.getColumn(ID_COLUMN_PERMISSIONS)
		val COLUMN_SIZE = table.columnModel.getColumn(ID_COLUMN_SIZE)
		val COLUMN_TITLE = table.columnModel.getColumn(ID_COLUMN_TITLE)

		COLUMN_ICON.apply {
			//headerValue = Texts2.COLUMN_ICON
			minWidth = 64
			maxWidth = 64
			preferredWidth = 64
			resizable = false
		}
		COLUMN_ID.apply {
			minWidth = 96
			preferredWidth = 96
			resizable = false
			cellRenderer = DefaultTableCellRenderer().apply {
				horizontalAlignment = JLabel.CENTER;
			}
		}
		COLUMN_TYPE.apply {
			minWidth = 80
			preferredWidth = 80
			resizable = false
			cellRenderer = DefaultTableCellRenderer().apply {
				horizontalAlignment = JLabel.CENTER;
			}
		}
		//COLUMN_WHERE.apply {
		//	width = 64
		//	minWidth = 64
		//	maxWidth = 64
		//	preferredWidth = 64
		//	resizable = false
		//	cellRenderer = DefaultTableCellRenderer().apply {
		//		horizontalAlignment = JLabel.CENTER;
		//	}
		//}
		COLUMN_VERSION.apply {
			minWidth = 64
			preferredWidth = 64
			resizable = false
			cellRenderer = DefaultTableCellRenderer().apply {
				horizontalAlignment = JLabel.CENTER;
			}
		}
		COLUMN_PERMISSIONS.apply {
			minWidth = 96
			preferredWidth = 96
			resizable = false
			cellRenderer = DefaultTableCellRenderer().apply {
				horizontalAlignment = JLabel.CENTER
			}
		}
		COLUMN_SIZE.apply {
			minWidth = 96
			preferredWidth = 96
			resizable = false
			cellRenderer = DefaultTableCellRenderer().apply {
				horizontalAlignment = JLabel.CENTER
			}
		}

		setSorter()
		//table.rowSorter = TableRowSorter<TableModel>(table.model).apply {
		//	setComparator(COLUMN_SIZE.modelIndex, { a, b -> (a as Comparable<Any>).compareTo((b as Comparable<Any>)) })
		//}

		COLUMN_TITLE.apply {
			width = 512
			preferredWidth = 512
			//resizable = false
		}

		table.font = Font(Font.MONOSPACED, Font.PLAIN, VitaOrganizerSettings.tableFontSize)

		//table.selectionModel.addListSelectionListener { e -> println(e) };

		table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
		table.columnModel.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

		table.addKeyListener(object : KeyAdapter() {
			override fun keyPressed(e: KeyEvent) {
				if (e.keyCode == KeyEvent.VK_ENTER) {
					showMenu()
				} else {
					super.keyPressed(e)
				}
			}
		})

		table.addMouseListener(object : MouseAdapter() {
			override fun mouseReleased(e: MouseEvent) {
				super.mouseReleased(e)
				table.clearSelection()

				val row = table.rowAtPoint(Point(e.x, e.y))
				if (row < 0 || row > table.rowCount)
					return;

				try {
					table.addRowSelectionInterval(row, row)
					showMenu()
				} catch (t: Throwable) {
					t.printStackTrace()
				}
			}
		})

		updateRowFilter(filter)
		//filter = "Plant"
	}

	fun getEntryAtRow(row: Int): CachedVpkEntry = model2.getValueAt(table.convertRowIndexToModel(row), 1) as CachedVpkEntry

	val currentEntry: CachedVpkEntry get() = getEntryAtRow(table.selectedRow)

	fun showMenuForRow(row: Int) {
		val rect = table.getCellRect(row, 1, true)
		showMenuAtFor(rect.x, rect.y + rect.height, getEntryAtRow(row))
	}

	open fun showMenuAtFor(x: Int, y: Int, entry: CachedVpkEntry) {
	}

	fun showMenu() {
		if (table.selectedRow !== -1)
			showMenuForRow(table.selectedRow)
	}

	override fun processKeyEvent(e: KeyEvent) {
		when (e.keyCode) {
			KeyEvent.VK_ENTER -> showMenu()
			else -> super.processKeyEvent(e)
		}
	}

	fun setEntries(games: List<CachedVpkEntry>) {
		val newRows = arrayListOf<Array<Any>>()

		for (entry in games.sortedBy { it.title }) {
			try {
				val entry2 = entry.entry
				val icon = entry2.icon0File
				val image = ImageIO.read(ByteArrayInputStream(icon.readBytes()))
				val psf = PSF.read(entry2.paramSfoFile.readBytes().stream)
				val extendedPermissions = entry.hasExtendedPermissions

				val type = when {
                    psf["CATEGORY"] == "gd" -> when {
                        psf["ATTRIBUTE"].toString().toInt() == 32768 -> "HOMEBREW"
                        else -> Texts.format("TYPE_GAME")
                    }
                    psf["CATEGORY"] == "gda" -> "SYSTEM"
					psf["CATEGORY"] == "gp" -> Texts.format("TYPE_UPDATE")
                    else -> psf["CATEGORY"]?.toString() ?: Texts.format("TYPE_UNKNOWN")
                }

				//if (entry.inVita && entry.inPC) {
				//	Texts.format("LOCATION_BOTH")
				//} else if (entry.inVita) {
				//	Texts.format("LOCATION_VITA")
				//} else if (entry.inPC) {
				//	Texts.format("LOCATION_PC")
				//} else {
				//	Texts.format("LOCATION_NONE")
				//},

				//println(psf)
				if (image != null) {
					newRows.add(arrayOf(
						ImageIcon(image.getScaledImage(64, 64)),
						entry,
						type,
						psf["APP_VER"] ?: psf["VERSION"] ?: Texts.format("UNKNOWN_VERSION"),
						(if (extendedPermissions) Texts.format("PERMISSIONS_UNSAFE") else Texts.format("PERMISSIONS_SAFE")),
						FileSize(entry.size),
						entry.title
					))
				}
			} catch (e: Throwable) {
				println("Error processing: ${entry.gameId} : ${entry.file}")
				e.printStackTrace()
			}
		}

		if (newRows.any()) {
			model2.rowCount = 0

			for (row in newRows)
				model2.addRow(row)

			model2.fireTableDataChanged()
		}

		//sorter.sort()

	}
}