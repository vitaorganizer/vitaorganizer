package com.soywiz.vitaorganizer

import util.open2
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableModel

object VitaOrganizer : JPanel(BorderLayout()) {
    @JvmStatic fun main(args: Array<String>) {
        SwingUtilities.invokeLater {
            //Create and set up the window.
            val frame = JFrame("VitaOrganized")
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.iconImage = ImageIO.read(ClassLoader.getSystemResource("vitafrontblk.jpg"))

            //Create and set up the content pane.
            val newContentPane = VitaOrganizer
            newContentPane.isOpaque = true //content panes must be opaque
            frame.contentPane = newContentPane

            //Display the window.
            frame.pack()
            frame.setLocationRelativeTo(null)
            frame.isVisible = true
        }
    }

    class EntryData(val psf: Map<String, Any>, val vpkFile: File) {
        val id = psf["TITLE_ID"].toString()
        val title = psf["TITLE"].toString()
        override fun toString(): String {
            return id
        }
    }


    private val DEBUG = true

    val model = object : DefaultTableModel() {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return false
        }
    }

    init {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        //val columnNames = arrayOf("Icon", "ID", "Title")

        //val data = arrayOf(arrayOf(JLabel("Kathy"), "Smith", "Snowboarding", 5, false), arrayOf("John", "Doe", "Rowing", 3, true), arrayOf("Sue", "Black", "Knitting", 2, false), arrayOf("Jane", "White", "Speed reading", 20, true), arrayOf("Joe", "Brown", "Pool", 10, false))

        val table = object : JTable(model) {
            val gameTitlePopup = JMenuItem("").apply {
                this.isEnabled = false
            }

            val popupMenu = object : JPopupMenu() {
                var entry: EntryData? = null

                init {
                    add(gameTitlePopup)
                    add(JSeparator())
                    add(JMenuItem("Delete from PSVita").apply {
                        addActionListener {
                            //JOptionPane.showMessageDialog(frame, "Right-click performed on table and choose DELETE")
                        }
                        this.isEnabled = false
                    })
                    add(JMenuItem("Send to PSVita").apply {
                        addActionListener {
                            //JOptionPane.showMessageDialog(frame, "Right-click performed on table and choose DELETE")
                            val entry = entry
                            if (entry != null) {
                                val zip = ZipFile(entry.vpkFile)
                                try {
                                    PsvitaDevice.uploadGame(entry.id, ZipFile(entry.vpkFile))
                                } catch (e: Throwable) {
                                    JOptionPane.showMessageDialog(VitaOrganizer, "${e.toString()}", "${e.message}", JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                    })
                }

                override fun show(invoker: Component?, x: Int, y: Int) {
                    gameTitlePopup.text = "${entry?.id} : ${entry?.title}"

                    super.show(invoker, x, y)
                }
            }

            init {
                this.componentPopupMenu = popupMenu
            }

            fun showMenuForRow(row: Int) {
                val rect = getCellRect(row, 1, true)
                val entry = dataModel.getValueAt(row, 1) as EntryData
                popupMenu.entry = entry

                popupMenu.show(this, rect.x, rect.y + rect.height)
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
        }
        table.preferredScrollableViewportSize = Dimension(640, 480)

        if (DEBUG) {
        }

        //Create the scroll pane and add the table to it.
        val scrollPane = JScrollPane(table)

        //Add the scroll pane to this panel.
        //val const = SpringLayout.Constraints()
        //const.setConstraint(SpringLayout.NORTH, Spring.constant(32, 32, 32))
        //const.height = Spring.constant(32, 32, 32)

        val selectFolderButton = JButton("Select folder...")

        add(selectFolderButton, SpringLayout.NORTH)
        add(scrollPane)

        table.rowHeight = 64

        selectFolderButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                val chooser = JFileChooser()
                chooser.currentDirectory = java.io.File(".")
                chooser.dialogTitle = "Select PsVita VPK folder"
                chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                chooser.isAcceptAllFileFilterUsed = false
                chooser.selectedFile = File(VitaOrganizerSettings.vpkFolder)
                val result = chooser.showOpenDialog(this@VitaOrganizer)
                if (result == JFileChooser.APPROVE_OPTION) {
                    VitaOrganizerSettings.vpkFolder = chooser.selectedFile.absolutePath
                    updateFileList()
                }
            }
        })

        //table.getColumnModel().getColumn(0).cellRenderer = JTable.IconRenderer()
        //(table.model as DefaultTableModel).addRow(arrayOf("John", "Doe", "Rowing", 3, true))

        table.fillsViewportHeight = true

        model.addColumn("Icon")
        model.addColumn("ID")
        model.addColumn("Title")

        //table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        table.getColumn("Icon").apply {
            width = 64
            minWidth = 64
            maxWidth = 64
            preferredWidth = 64
            resizable = false
        }
        table.getColumn("Title").apply {
            width = 512
            preferredWidth = 512
            //resizable = false
        }

        table.selectionModel.addListSelectionListener(object : ListSelectionListener {
            override fun valueChanged(e: ListSelectionEvent?) {
                println(e)
            }
        });

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
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                val row = table.rowAtPoint(Point(e.x, e.y))
                if (row >= 0) table.showMenu()
            }
        })

        updateFileList()
    }

    fun updateFileList() {
        while (model.rowCount > 0) {
            model.removeRow(model.rowCount - 1)
        }
        for (vpkFile in File(VitaOrganizerSettings.vpkFolder).listFiles().filter { it.extension.toLowerCase() == "vpk" }) {
            try {
                val zip = ZipFile(vpkFile)
                val imageData = zip.getInputStream(zip.getEntry("sce_sys/icon0.png")).readBytes()
                val paramSfoData = zip.getInputStream(zip.getEntry("sce_sys/param.sfo")).readBytes()
                val image = ImageIO.read(ByteArrayInputStream(imageData))
                val psf = PSF.read(paramSfoData.open2("r"))

                println(psf)

                model.addRow(arrayOf(
                        ImageIcon(getScaledImage(image, 64, 64)),
                        EntryData(psf, vpkFile),
                        psf["TITLE"].toString()
                ))
            } catch (e: Throwable) {

            }
        }

        model.fireTableDataChanged()

    }

    private fun getScaledImage(srcImg: Image, w: Int, h: Int): Image {
        val resizedImg = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g2 = resizedImg.createGraphics()

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2.drawImage(srcImg, 0, 0, w, h, null)
        g2.dispose()

        return resizedImg
    }

    private fun printDebugData(table: JTable) {
        val numRows = table.rowCount
        val numCols = table.columnCount
        val model = table.model

        println("Value of data: ")
        for (i in 0..numRows - 1) {
            print("    row $i:")
            for (j in 0..numCols - 1) {
                print("  " + model.getValueAt(i, j))
            }
            println()
        }
        println("--------------------------")
    }
}