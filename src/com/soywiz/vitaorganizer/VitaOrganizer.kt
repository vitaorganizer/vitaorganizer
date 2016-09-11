package com.soywiz.vitaorganizer

import com.soywiz.util.open2
import com.soywiz.util.stream
import com.soywiz.vitaorganizer.ext.getBytes
import com.soywiz.vitaorganizer.ext.showDialog
import com.soywiz.vitaorganizer.popups.KeyValueViewerFrame
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds
import java.util.*
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

class VitaOrganizer(val frame: JFrame) : JPanel(BorderLayout()), StatusUpdater {
	companion object {
		@JvmStatic fun main(args: Array<String>) {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

			println("Locale.getDefault():" + Locale.getDefault())
			//PsvitaDevice.discoverIp()
			//SwingUtilities.invokeLater {
			//Create and set up the window.
			val frame = JFrame("VitaOrganizer $currentVersion")

			frame.jMenuBar = JMenuBar().apply {
				add(JMenu(Texts.format("MENU_FILE")).apply {
					add(JMenuItem("Select folder..."))
					add(JMenuItem("Refresh"))
					add(JSeparator())
					add(JMenuItem("Exit"))
				})
				add(JMenu(Texts.format("MENU_HELP")).apply {
					add(JMenuItem("Open website"))
					add(JSeparator())
					add(JMenuItem("Check for updates..."))
					add(JMenuItem("About..."))
				})
			}

			frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
			frame.iconImage = ImageIO.read(getResourceURL("icon.png"))

			//Create and set up the content pane.
			val newContentPane = VitaOrganizer(frame)
			newContentPane.isOpaque = true //content panes must be opaque
			frame.contentPane = newContentPane

			//Display the window.
			frame.pack()
			frame.setLocationRelativeTo(null)
			frame.isVisible = true
			//}

		}

		@JvmStatic fun getResourceURL(name: String) = ClassLoader.getSystemResource(name)
		@JvmStatic fun getResourceBytes(name: String) = try {
			ClassLoader.getSystemResource(name).readBytes()
		} catch (e: Throwable) {
			null
		}

		@JvmStatic fun getResourceString(name: String) = try {
			ClassLoader.getSystemResource(name).readText()
		} catch (e: Throwable) {
			null
		}

		@JvmStatic val currentVersion: String get() = getResourceString("currentVersion.txt") ?: "unknown"
	}

	class GameEntry(val gameId: String) {
		val entry = VitaOrganizerCache.entry(gameId)
		val psf by lazy {
			try {
				PSF.read(entry.paramSfoFile.readBytes().stream)
			} catch (e: Throwable) {
				mapOf<String, Any>()
			}
		}
		val id by lazy { psf["TITLE_ID"].toString() }
		val title by lazy { psf["TITLE"].toString() }
		var inVita = false
		var inPC = false
		val vpkFile: String? get() = entry.pathFile.readText(Charsets.UTF_8)
		val size: Long by lazy {
			try {
				entry.sizeFile.readText().toLong()
			} catch (e: Throwable) {
				0L
			}
		}

		override fun toString(): String = id
	}

	val VPK_GAME_IDS = hashSetOf<String>()
	val VITA_GAME_IDS = hashSetOf<String>()

	val statusLabel = JLabel(Texts.format("STEP_STARTED"))

	override fun updateStatus(status: String) {
		SwingUtilities.invokeLater {
			statusLabel.text = status
		}
	}

	fun updateEntries() {
		val ALL_GAME_IDS = LinkedHashMap<String, GameEntry>()

		fun getGameEntryById(gameId: String) = ALL_GAME_IDS.getOrPut(gameId) { GameEntry(gameId) }

		synchronized(VPK_GAME_IDS) {
			for (gameId in VPK_GAME_IDS) getGameEntryById(gameId).inPC = true
		}
		synchronized(VITA_GAME_IDS) {
			for (gameId in VITA_GAME_IDS) getGameEntryById(gameId).inVita = true
		}

		val newRows = arrayListOf<Array<Any>>()

		table.updateEntries(ALL_GAME_IDS.values.toList())
	}

	val table = object : GameListTable() {
		val dialog = this@VitaOrganizer
		val gameTitlePopup = JMenuItem("").apply {
			this.isEnabled = false
		}

		val popupMenu = object : JPopupMenu() {
			var entry: GameEntry? = null

			val deleteFromVita = JMenuItem(Texts.format("DELETE_FROM_PSVITA_ACTION")).apply {
				addActionListener {
					val entry = entry
					if (entry != null) {
						val info = mapOf("title" to entry.title)
						JOptionPane.showConfirmDialog(
							dialog,
							Texts.formatMap("DELETE_FROM_PSVITA_MESSAGE", info),
							Texts.formatMap("DELETE_FROM_PSVITA_TITLE", info),
							JOptionPane.OK_CANCEL_OPTION, JOptionPane.OK_OPTION
						)
					}
				}
				this.isEnabled = false
			}

			fun createSmallVpk(zip: ZipFile): ByteArray {
				// out put file
				val outBytes = ByteArrayOutputStream()
				val out = ZipOutputStream(outBytes)
				out.setLevel(Deflater.DEFAULT_COMPRESSION)

				// name the file inside the zip  file
				for (e in zip.entries()) {
					if (e.name == "eboot.bin" || e.name.startsWith("sce_sys/")) {
						out.putNextEntry(ZipEntry(e.name))
						out.write(zip.getInputStream(e).readBytes())
					}
				}

				out.close()

				return outBytes.toByteArray()
			}

			val sendVpkToVita = JMenuItem(Texts.format("SEND_PROMOTING_VPK_TO_VITA_ACTION")).apply {
				addActionListener {
					//JOptionPane.showMessageDialog(frame, "Right-click performed on table and choose DELETE")
					val entry = entry
					if (entry != null) {
						Thread {
							val zip = ZipFile(entry.vpkFile)

							updateStatus(Texts.format("STEP_CHECKING_EBOOT_PERMISSIONS"))

							if (EbootBin.hasExtendedPermissions(zip.getBytes("eboot.bin").open2("r"))) {
								val result = JOptionPane.showConfirmDialog(
									this@VitaOrganizer,
									"Game ${entry.id} requires extended permissions.\nAre you sure you want to install it. It could damage your device?",
									"WARNING!",
									JOptionPane.YES_NO_OPTION
								)

								if (result != JOptionPane.YES_OPTION) {
									throw InterruptedException("Not accepted installing game with extended permissions")
								}
							}

							updateStatus(Texts.format("STEP_GENERATING_SMALL_VPK_FOR_PROMOTING"))

							val vpkPath = "ux0:/organizer/${entry.id}.VPK"

							//val zip = ZipFile(entry.vpkFile)
							try {
								val vpkData = createSmallVpk(zip)

								PsvitaDevice.uploadFile("/$vpkPath", vpkData) { status ->
									updateStatus(Texts.format("STEP_UPLOADING_VPK_FOR_PROMOTING", "current" to status.currentSize, "total" to status.totalSize))
								}

								//statusLabel.text = "Processing game ${vitaGameCount + 1}/${vitaGameIds.size} ($gameId)..."
							} catch (e: Throwable) {
								e.printStackTrace()
								JOptionPane.showMessageDialog(this@VitaOrganizer, "${e.toString()}", "${e.message}", JOptionPane.ERROR_MESSAGE);
							}
							SwingUtilities.invokeLater {
								statusLabel.text = "Sent game vpk ${entry.id}"
								JOptionPane.showMessageDialog(
									this@VitaOrganizer,
									"Now use VitaShell to install\n$vpkPath\n\nAfer that active ftp again and use this program to Send Data to PSVita",
									Texts.format("INFORMATION"),
									JOptionPane.INFORMATION_MESSAGE
								);
							}

							zip.close()
						}.start()
					}
				}
			}

			val sendToVita = JMenuItem("Send Data to PSVita").apply {
				addActionListener {
					//JOptionPane.showMessageDialog(frame, "Right-click performed on table and choose DELETE")
					val entry = entry
					if (entry != null) {
						Thread {
							updateStatus("Sending game ${entry.id}...")
							//val zip = ZipFile(entry.vpkFile)
							try {
								PsvitaDevice.uploadGame(entry.id, ZipFile(entry.vpkFile), filter = { path ->
									// Skip files already installed in the VPK
									if (path == "eboot.bin" || path.startsWith("sce_sys/")) {
										false
									} else {
										true
									}
								}) { status ->
									//println("$status")
									updateStatus("Uploading ${entry.id} :: ${status.fileRange} :: ${status.sizeRange}")
								}
								//statusLabel.text = "Processing game ${vitaGameCount + 1}/${vitaGameIds.size} ($gameId)..."
							} catch (e: Throwable) {
								JOptionPane.showMessageDialog(this@VitaOrganizer, "${e.toString()}", "${e.message}", JOptionPane.ERROR_MESSAGE);
							}
							updateStatus("Sent game data ${entry.id}")
							SwingUtilities.invokeLater {
								JOptionPane.showMessageDialog(this@VitaOrganizer, "Game ${entry.id} sent successfully", "Actions", JOptionPane.INFORMATION_MESSAGE);
							}
						}.start()
					}
				}
			}

			val sendToVita1Step = JMenuItem("Send Full App to PSVita in just one 1-step (Requires VitaShell >= 0.9.5)").apply {
				addActionListener {
					//JOptionPane.showMessageDialog(frame, "Right-click performed on table and choose DELETE")
					val entry = entry
					if (entry != null) {
						Thread {
							val zip = ZipFile(entry.vpkFile)

							SwingUtilities.invokeLater {
								statusLabel.text = "Checking eboot permissions..."
							}

							if (EbootBin.hasExtendedPermissions(zip.getBytes("eboot.bin").open2("r"))) {
								val result = JOptionPane.showConfirmDialog(this@VitaOrganizer, "Game ${entry.id} requires extended permissions.\nAre you sure you want to install it. It could damage your device?", "WARNING!", JOptionPane.YES_NO_OPTION);
								if (result != JOptionPane.YES_OPTION) {
									throw InterruptedException("Not accepted installing game with extended permissions")
								}
							}

							SwingUtilities.invokeLater {
								statusLabel.text = "Generating small VPK for promoting..."
							}

							val vpkPath = "ux0:/organizer/${entry.id}.VPK"

							//val zip = ZipFile(entry.vpkFile)
							try {
								val vpkData = createSmallVpk(zip)

								PsvitaDevice.uploadFile("/$vpkPath", vpkData) { status ->
									SwingUtilities.invokeLater {
										statusLabel.text = "Uploading VPK for promoting (${status.sizeRange})..."
									}
								}

								//statusLabel.text = "Processing game ${vitaGameCount + 1}/${vitaGameIds.size} ($gameId)..."
							} catch (e: Throwable) {
								JOptionPane.showMessageDialog(this@VitaOrganizer, "${e.toString()}", "${e.message}", JOptionPane.ERROR_MESSAGE);
							}

							updateStatus("Promoting VPK (this could take a while)...")

							PsvitaDevice.promoteVpk("ux0:organizer/${entry.id}.VPK")

							PsvitaDevice.removeFile("/$vpkPath")

							updateStatus("Sending game ${entry.id}...")
							try {
								PsvitaDevice.uploadGame(entry.id, zip, filter = { path ->
									// Skip files already installed in the VPK
									if (path == "eboot.bin" || path.startsWith("sce_sys/")) {
										false
									} else {
										true
									}
								}) { status ->
									//println("$status")
									updateStatus("Uploading ${entry.id} :: ${status.fileRange} :: ${status.sizeRange}")
								}
								//statusLabel.text = "Processing game ${vitaGameCount + 1}/${vitaGameIds.size} ($gameId)..."

								SwingUtilities.invokeLater {
									statusLabel.text = "Sent game data ${entry.id}"
									JOptionPane.showMessageDialog(this@VitaOrganizer, "Game ${entry.id} sent successfully", "Actions", JOptionPane.INFORMATION_MESSAGE);
								}
							} catch (e: Throwable) {
								println("Error uploading game")
								e.printStackTrace()
								JOptionPane.showMessageDialog(this@VitaOrganizer, "${e.toString()}", "${e.message}", JOptionPane.ERROR_MESSAGE);
							}

						}.start()
					}
				}
			}

			init {
				add(gameTitlePopup)
				add(JSeparator())
				add(JMenuItem(Texts.format("MENU_SHOW_PSF")).apply {
					addActionListener {
						val entry = entry
						if (entry != null) {
							frame.showDialog(KeyValueViewerFrame(Texts.format("PSF_VIEWER_TITLE", "id" to entry.id, "title" to entry.title), entry.psf))
						}
					}
				})
				add(JSeparator())
				//add(deleteFromVita)
				add(sendVpkToVita)
				add(sendToVita)
				add(JSeparator())
				add(sendToVita1Step)
			}

			override fun show(invoker: Component?, x: Int, y: Int) {
				val entry = entry
				gameTitlePopup.text = "UNKNOWN"
				deleteFromVita.isEnabled = false
				sendToVita.isEnabled = false
				sendToVita1Step.isEnabled = false

				if (entry != null) {
					gameTitlePopup.text = "${entry.id} : ${entry.title}"
					deleteFromVita.isEnabled = entry.inVita
					//sendToVita.isEnabled = !entry.inVita
					sendToVita.isEnabled = entry.inPC
					sendToVita1Step.isEnabled = entry.inPC
				}

				super.show(invoker, x, y)
			}
		}

		override fun showMenuAtFor(x: Int, y: Int, entry: GameEntry) {
			popupMenu.entry = entry
			popupMenu.show(this, x, y)
		}

		init {
			this.componentPopupMenu = popupMenu
		}
	}

	init {
		//val columnNames = arrayOf("Icon", "ID", "Title")

		//val data = arrayOf(arrayOf(JLabel("Kathy"), "Smith", "Snowboarding", 5, false), arrayOf("John", "Doe", "Rowing", 3, true), arrayOf("Sue", "Black", "Knitting", 2, false), arrayOf("Jane", "White", "Speed reading", 20, true), arrayOf("Joe", "Brown", "Pool", 10, false))


		table.preferredScrollableViewportSize = Dimension(800, 600)
		//table.rowSelectionAllowed = false
		//table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

		//Create the scroll pane and add the table to it.
		val scrollPane = JScrollPane(table)

		//Add the scroll pane to this panel.
		//val const = SpringLayout.Constraints()
		//const.setConstraint(SpringLayout.NORTH, Spring.constant(32, 32, 32))
		//const.height = Spring.constant(32, 32, 32)

		val footer = JPanel().apply {
			add(statusLabel)
		}

		val header = JPanel(FlowLayout(FlowLayout.LEFT)).apply {

			add(JButton("Select folder...").apply {
				this.addMouseListener(object : MouseAdapter() {
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
			})

			add(JButton("Refresh").apply {
				this.addMouseListener(object : MouseAdapter() {
					override fun mouseClicked(e: MouseEvent?) {
						updateFileList()
					}
				})
			})

			val connectText = "Connect to PsVita..."
			val disconnectText = "Disconnect from %s"
			var connected = false
			val connectAddress = object : JTextField(VitaOrganizerSettings.lastDeviceIp) {
				init {
					font = Font(Font.MONOSPACED, Font.PLAIN, 14)
				}

				override fun processKeyEvent(e: KeyEvent?) {
					super.processKeyEvent(e)
					VitaOrganizerSettings.lastDeviceIp = this.text
				}
			}

			connectAddress.addActionListener {
				println("aaa")
			}

			val connectButton = object : JButton(connectText) {
				val button = this

				fun disconnect() {
					connected = false
					button.text = connectText
					synchronized(VITA_GAME_IDS) {
						VITA_GAME_IDS.clear()
					}
					updateEntries()
					statusLabel.text = "Disconnected"
				}

				fun connect(ip: String) {
					connected = true
					VitaOrganizerSettings.lastDeviceIp = ip
					PsvitaDevice.setIp(ip, 1337)
					button.text = disconnectText.format(ip)
					connectAddress.text = ip
					synchronized(VITA_GAME_IDS) {
						VITA_GAME_IDS.clear()
					}
					var done = false
					var updated = false
					Thread {
						try {
							var vitaGameCount = 0
							val vitaGameIds = PsvitaDevice.getGameIds()
							for (gameId in vitaGameIds) {
								updateStatus("Processing game ${vitaGameCount + 1}/${vitaGameIds.size} ($gameId)...")
								//println(gameId)
								try {
									PsvitaDevice.getParamSfoCached(gameId)
									PsvitaDevice.getGameIconCached(gameId)
									val entry2 = VitaOrganizerCache.entry(gameId)
									val sizeFile = entry2.sizeFile
									if (!sizeFile.exists()) {
										sizeFile.writeText("" + PsvitaDevice.getGameSize(gameId))
									}

									if (!entry2.permissionsFile.exists()) {
										val ebootBin = PsvitaDevice.downloadEbootBin(gameId)
										try {
											entry2.permissionsFile.writeText("" + EbootBin.hasExtendedPermissions(ebootBin.open2("r")))
										} catch (e: Throwable) {
											entry2.permissionsFile.writeText("true")
										}
									}

									synchronized(VITA_GAME_IDS) {
										VITA_GAME_IDS += gameId
									}
									updated = true
									//val entry = getGameEntryById(gameId)
									//entry.inVita = true
								} catch (e: Throwable) {
									e.printStackTrace()
								}
								vitaGameCount++
							}
						} finally {
							done = true
							updated = true
						}

						updateStatus("Connected")
					}.start()

					Thread {
						do {
							//println("a")
							while (!updated) {
								//println("b")
								Thread.sleep(100L)
							}
							updated = false
							updateEntries()
						} while (!done)


					}.start()
				}

				init {
					val button = this
					addMouseListener(object : MouseAdapter() {
						override fun mouseClicked(e: MouseEvent?) {
							if (connected) {
								this@VitaOrganizer.updateStatus("Disconnecting...")
								disconnect()
							} else {
								this@VitaOrganizer.updateStatus("Connecting...")
								button.button.isEnabled = false

								connect(VitaOrganizerSettings.lastDeviceIp)
								button.button.isEnabled = true

								/*
								if (PsvitaDevice.checkAddress(VitaOrganizerSettings.lastDeviceIp)) {
								} else {
									Thread {
										val ips = PsvitaDevice.discoverIp()
										println("Discovered ips: $ips")
										if (ips.size >= 1) {
											connect(ips.first())
										}
										button.button.isEnabled = true
									}.start()
								}
								*/
							}
						}
					})
				}
			}

			val checkUpdatesButton = object : JButton("Check for updates...") {

			}

			checkUpdatesButton.addMouseListener(object : MouseAdapter() {
				override fun mouseClicked(e: MouseEvent) {
					val text = URL("https://raw.githubusercontent.com/soywiz/vitaorganizer/master/lastVersion.txt").readText()
					val parts = text.lines()
					val lastVersion = parts[0]
					val lastVersionUrl = parts[1]
					if (lastVersion == currentVersion) {
						JOptionPane.showMessageDialog(this@VitaOrganizer, "You have the lastest version: $currentVersion", "Actions", JOptionPane.INFORMATION_MESSAGE);
					} else {
						val result = JOptionPane.showConfirmDialog(this@VitaOrganizer, "There is a new version: $lastVersion\nYou have: $currentVersion\nWant to download last version?", "Actions", JOptionPane.YES_NO_OPTION);
						if (result == JOptionPane.OK_OPTION) {
							openWebpage(URL(lastVersionUrl))
						}
					}
					println(parts)
				}
			})
			add(connectButton)
			add(connectAddress)
			add(checkUpdatesButton)
		}

		add(header, SpringLayout.NORTH)
		add(scrollPane)
		add(footer, SpringLayout.SOUTH)

		updateFileList()
	}

	fun openWebpage(uri: URI) {
		val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(uri)
			} catch (e: Exception) {
				e.printStackTrace()
			}

		}
	}

	fun openWebpage(url: URL) {
		try {
			openWebpage(url.toURI())
		} catch (e: URISyntaxException) {
			e.printStackTrace()
		}

	}

	fun updateFileList() {
		Thread {
			synchronized(VPK_GAME_IDS) {
				VPK_GAME_IDS.clear()
			}
			val vpkFiles = File(VitaOrganizerSettings.vpkFolder).listFiles().filter { it.name.toLowerCase().endsWith(".vpk") }
			updateStatus(Texts.format("STEP_ANALYZING_FILES", "folder" to VitaOrganizerSettings.vpkFolder))
			var count = 0
			for (vpkFile in File(VitaOrganizerSettings.vpkFolder).listFiles().filter { it.name.toLowerCase().endsWith(".vpk") }) {
				//println(vpkFile)
				updateStatus(Texts.format("STEP_ANALYZING_ITEM", "name" to vpkFile.name, "current" to count + 1, "total" to vpkFiles.size))
				try {
					val zip = ZipFile(vpkFile)
					val paramSfoData = zip.getBytes("sce_sys/param.sfo")

					val psf = PSF.read(paramSfoData.open2("r"))
					val gameId = psf["TITLE_ID"].toString()

					val entry = VitaOrganizerCache.entry(gameId)

					if (!entry.icon0File.exists()) {
						entry.icon0File.writeBytes(zip.getInputStream(zip.getEntry("sce_sys/icon0.png")).readBytes())
					}
					if (!entry.paramSfoFile.exists()) {
						entry.paramSfoFile.writeBytes(paramSfoData)
					}
					if (!entry.sizeFile.exists()) {
						val uncompressedSize = ZipFile(vpkFile).entries().toList().map { it.size }.sum()
						entry.sizeFile.writeText("" + uncompressedSize)
					}
					if (!entry.permissionsFile.exists()) {
						val ebootBinData = zip.getBytes("eboot.bin")
						entry.permissionsFile.writeText("" + EbootBin.hasExtendedPermissions(ebootBinData.open2("r")))
					}
					entry.pathFile.writeBytes(vpkFile.absolutePath.toByteArray(Charsets.UTF_8))
					synchronized(VPK_GAME_IDS) {
						VPK_GAME_IDS += gameId
					}
					//getGameEntryById(gameId).inPC = true
				} catch (e: Throwable) {
					println("Error processing ${vpkFile.name}")
					e.printStackTrace()
				}
			}
			updateStatus(Texts.format("STEP_DONE"))
			updateEntries()
		}.start()
	}



	fun fileWatchFolder(path: String) {
		val watcher = FileSystems.getDefault().newWatchService()
		val dir = FileSystems.getDefault().getPath(path)
		try {

			val key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

		} catch (x: IOException) {
			System.err.println(x);
		}
	}
}
