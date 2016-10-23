package com.soywiz.vitaorganizer

import com.soywiz.util.*
import com.soywiz.vitaorganizer.ext.*
import com.soywiz.vitaorganizer.popups.AboutFrame
import com.soywiz.vitaorganizer.popups.KeyValueViewerFrame
import com.soywiz.vitaorganizer.tasks.*
import java.awt.*
import java.awt.event.*
import java.io.File
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter

class VitaOrganizer : JPanel(BorderLayout()), StatusUpdater {
	companion object {
		lateinit var instance: VitaOrganizer

		@JvmStatic fun main(args: Array<String>) {
			VitaOrganizer()
			VitaOrganizerSettings.init()
			VitaOrganizerSettings.WLAN_SSID
			VitaOrganizerSettings.WLAN_PASS
			instance.start()
		}
	}

	init {
		VitaOrganizer.instance = this@VitaOrganizer
	}

	val vitaOrganizer = this@VitaOrganizer
	val localTasks = VitaTaskQueue(this)
	val remoteTasks = VitaTaskQueue(this)
	val hotspotQueue = VitaTaskQueue(this)

	val runningTasks: Boolean get() = localTasks.running || remoteTasks.running || hotspotQueue.running

	init {
		Texts.setLanguage(VitaOrganizerSettings.LANGUAGE_LOCALE)
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
	}

	val frame = JFrame("VitaOrganizer ${VitaOrganizerVersion.currentVersion}").apply {
		defaultCloseOperation = JFrame.EXIT_ON_CLOSE
		iconImage = ImageIO.read(getResourceURL("com/soywiz/vitaorganizer/icon.png"))
	}

	fun start() {
		//frame.pack()
		frame.setSize(VitaOrganizerSettings.WINDOW_WIDTH, VitaOrganizerSettings.WINDOW_HEIGHT)
		frame.setLocationRelativeTo(null)
		frame.isVisible = true
	}

	init {
		//Create and set up the content pane.
		val newContentPane = this
		newContentPane.isOpaque = true //content panes must be opaque
		frame.contentPane = newContentPane

		//Display the window.

		//}
	}

	val VPK_GAME_FILES = hashSetOf<File>()

	val statusLabel = JLabel(Texts.format("STEP_STARTED"))

	override fun updateStatus(status: String) {
		//println(status)
		SwingUtilities.invokeLater {
			statusLabel.text = status
		}
	}

	fun updateEntries() {
		table.setEntries(synchronized(VPK_GAME_FILES) { VPK_GAME_FILES.map { CachedVpkEntry(it) } })
	}

	fun showFileInExplorerOrFinder(file: File) {
		if (!file.exists())
			return

		if (OS.isWindows) {
			ProcessBuilder("explorer.exe", "/select,", file.absolutePath).start().waitFor()
		} else {
			ProcessBuilder("open", "-R", file.absolutePath).start().waitFor()
		}
	}

	val table = object : GameListTable() {
		val dialog = this@VitaOrganizer
		val gameTitlePopup = JMenuItem("").apply {
			isEnabled = false
			foreground = Color(64, 0, 255);
			font = font.deriveFont(Font.BOLD);
		}
		val gamePathMenuItem = JMenuItem("").apply {
		}
		val gameDumperVersionPopup = JMenuItem("").apply {
			this.isEnabled = false
		}
		val gameCompressionLevelPopup = JMenuItem("").apply {
			this.isEnabled = false
		}

		val popupMenu = object : JPopupMenu() {
			var entry: CachedVpkEntry? = null

			val sendVpkToVita = JMenuItem(Texts.format("SEND_PROMOTING_VPK_TO_VITA_ACTION")).action {
				if (entry != null) remoteTasks.queue(SendPromotingVpkToVitaTask(vitaOrganizer, entry!!.vpkLocalVpkFile!!))
			}

			val sendDataToVita = JMenuItem(Texts.format("SEND_DATA_TO_VITA_ACTION")).action {
				if (entry != null) remoteTasks.queue(SendDataToVitaTask(vitaOrganizer, entry!!.vpkLocalVpkFile!!))
			}

			val sendToVita1Step = JMenuItem(Texts.format("SEND_FULL_APP_TO_VITA_ACTION")).action {
				if (entry != null) remoteTasks.queue(OneStepToVitaTask(vitaOrganizer, entry!!.vpkLocalVpkFile!!))
			}

			val showInFilebrowser = JMenuItem(if (OS.isWindows) Texts.format("MENU_SHOW_EXPLORER") else Texts.format("MENU_SHOW_FINDER")).action {
				if (entry != null) showFileInExplorerOrFinder(entry!!.vpkLocalFile!!)
			}

			val repackVpk = JMenuItem(Texts.format("MENU_REPACK")).action {
				if (entry != null) remoteTasks.queue(RepackVpkTask(vitaOrganizer, entry!!, setSecure = true))
			}

			val showPSF = JMenuItem(Texts.format("MENU_SHOW_PSF")).action {
				if (entry != null) {
					frame.showFrame(KeyValueViewerFrame(Texts.format("PSF_VIEWER_TITLE", "id" to entry!!.id, "title" to entry!!.title), entry!!.psf, formatter = { key, value ->
						when (key) {
							"ATTRIBUTE", "ATTRIBUTE2", "ATTRIBUTE_MINOR" -> {
								//show as hex
								String.format("0x%X", value.toString().toInt())
                            }
							else -> "$value"
						}
					}))
				}
			}

			init {
				add(gameTitlePopup)
				add(gamePathMenuItem.action {
					if (entry != null) showFileInExplorerOrFinder(entry!!.vpkLocalFile!!)
				})
				add(JSeparator())
				add(gameDumperVersionPopup)
				add(gameCompressionLevelPopup)
				add(JSeparator())
				add(showInFilebrowser)
				add(showPSF)
				add(repackVpk)
				add(JMenu(Texts.format("METHOD1_INFO")).apply {
					//isEnabled = false
					add(sendVpkToVita)
					add(sendDataToVita)
				})
				add(JSeparator())
				//add(JMenuItem(Texts.format("METHOD2_INFO")).apply {
				//	isEnabled = false
				//})
				add(sendToVita1Step)
			}

			override fun show(invoker: Component?, x: Int, y: Int) {
				val entry = entry
				gameTitlePopup.text = Texts.format("UNKNOWN_VERSION")
				gameDumperVersionPopup.text = Texts.format("UNKNOWN_VERSION")
				gameCompressionLevelPopup.text = Texts.format("UNKNOWN_VERSION")

				if (entry != null) {
					gamePathMenuItem.text = entry.file.canonicalPath
					gameDumperVersionPopup.text = Texts.format("DUMPER_VERSION", "version" to entry.dumperVersion)
					gameCompressionLevelPopup.text = Texts.format("COMPRESSION_LEVEL", "level" to entry.compressionLevel)
					gameTitlePopup.text = "${entry.id} : ${entry.title}"
				}

				super.show(invoker, x, y)
			}
		}

		override fun showMenuAtFor(x: Int, y: Int, entry: CachedVpkEntry) {
			popupMenu.entry = entry
			popupMenu.show(this.table, x, y)
		}

		init {
			//this.componentPopupMenu = popupMenu
		}
	}

	fun selectFolder() {
		val chooser = JFileChooser()
		chooser.currentDirectory = File(VitaOrganizerSettings.vpkFolder)
		chooser.dialogTitle = Texts.format("SELECT_PSVITA_VPK_FOLDER")
		chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
		chooser.isAcceptAllFileFilterUsed = false
		//chooser.selectedFile = File(VitaOrganizerSettings.vpkFolder)
		val result = chooser.showOpenDialog(this@VitaOrganizer)
		if (result == JFileChooser.APPROVE_OPTION) {
			VitaOrganizerSettings.vpkFolder = chooser.selectedFile.absolutePath
			updateFileList()
		}
	}

	val filterTextField = object : JTextField("") {
		init {
			font = Font(Font.MONOSPACED, Font.PLAIN, 14)
			columns = 16
		}

		override fun processKeyEvent(e: KeyEvent?) {
			super.processKeyEvent(e)
			table.filter = this.text
		}
	}.apply {
		//addActionListener {
		//	println("aaa")
		//}
	}

	fun setLanguageTextCheckingRunning(name: String) {
		if (runningTasks) {
			val confirmed = JOptionPane.showConfirmDialog(null, Texts.format("CONFIRM_EXIT_TEXT"), Texts.format("CONFIRM_EXIT_TITLE"), JOptionPane.YES_NO_OPTION)

			if (confirmed != JOptionPane.YES_OPTION) {
				frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
				return
			}
		}

		VitaOrganizerSettings.LANGUAGE = name
		restart()
	}

	fun restart() {
		this.frame.isVisible = false
		this.frame.dispose()
		VitaOrganizer().start()
	}

	init {
		val vitaOrganizer = this

		frame.jMenuBar = JMenuBar().apply {
			add(JMenu(Texts.format("MENU_FILE")).apply {
				add(JMenuItem(Texts.format("MENU_REFRESH"), Icons.REFRESH).action {
					updateFileList()
				})
				add(JMenuItem(Texts.format("MENU_SELECT_FOLDER"), Icons.OPEN_FOLDER).action {
					selectFolder()
				})
				add(JMenuItem(Texts.format("MENU_INSTALL_VPK"), Icons.INSTALL).action {
					val chooser = JFileChooser()
					chooser.currentDirectory = File(VitaOrganizerSettings.vpkFolder)
					chooser.dialogTitle = Texts.format("SELECT_PSVITA_VPK_FOLDER")
					chooser.fileFilter = FileNameExtensionFilter(Texts.format("FILEFILTER_DESC_VPK_FILES"), "vpk")
					chooser.fileSelectionMode = JFileChooser.FILES_ONLY
					//chooser.isAcceptAllFileFilterUsed = false
					//chooser.selectedFile = File(VitaOrganizerSettings.vpkFolder)
					val result = chooser.showOpenDialog(this@VitaOrganizer)
					if (result == JFileChooser.APPROVE_OPTION) {
						remoteTasks.queue(OneStepToVitaTask(this@VitaOrganizer, VpkFile(chooser.selectedFile)))
					}
				})
				add(JMenuItem(Texts.format("MENU_CREATE_VPK_FROM_MAIDUMP_FOLDER"), Icons.MAIDUMP).action {
					val chooser = JFileChooser()
					chooser.currentDirectory = File(VitaOrganizerSettings.vpkFolder)
					chooser.dialogTitle = Texts.format("SELECT_PSVITA_VPK_FOLDER")
					chooser.fileSelectionMode = JFileChooser.FILES_ONLY
					chooser.fileFilter = object : FileFilter() {
						override fun accept(f: File): Boolean = f.isDirectory || f.name.toLowerCase() == "eboot.bin"
						override fun getDescription(): String = "EBOOT.BIN"
					}
					chooser.isAcceptAllFileFilterUsed = false
					//chooser.selectedFile = File(VitaOrganizerSettings.vpkFolder)
					val result = chooser.showOpenDialog(this@VitaOrganizer)
					if (result == JFileChooser.APPROVE_OPTION) {
						val root = chooser.selectedFile.parentFile
						//val ebootBinFile = root["eboot.bin"]
						val paramSfoFile = root["sce_sys/param.sfo"]
						val psfInfo = PSF.read(paramSfoFile.readBytes().open2("r"))
						val TITLE_ID = psfInfo["TITLE_ID"]?.toString() ?: invalidOp("Can't find TITLE_ID")
						val TITLE_ID_SECURED = File(TITLE_ID).name

						localTasks.queue(CreateVpkFromFolderVitaTask(instance, chooser.selectedFile.parentFile, File(VitaOrganizerSettings.vpkFolder)["$TITLE_ID_SECURED.vpk"]))
					}
				})
				add(JSeparator())
				add(JMenuItem(Texts.format("MENU_EXIT")).action {
					System.exit(0)
				})
			})
			add(JMenu(Texts.format("MENU_SETTINGS")).apply {
				add(JMenu(Texts.format("MENU_LANGUAGES")).apply {
					icon = Icons.TRANSLATIONS
					add(JRadioButtonMenuItem(Texts.format("MENU_LANGUAGE_AUTODETECT")).apply {
						this.isSelected = VitaOrganizerSettings.isLanguageAutodetect
					}.action {
						setLanguageTextCheckingRunning("auto")
					})
					add(JSeparator())
					for (l in Texts.SUPPORTED_LOCALES) {
						val lrb = JRadioButtonMenuItem(l.getDisplayLanguage(l).capitalize()).apply {
							this.isSelected = VitaOrganizerSettings.LANGUAGE_LOCALE == l
						}
						//languageList[l.language] = lrb
						add(lrb).action {
							setLanguageTextCheckingRunning(l.language)
						}
					}
					Unit
				})
				add(JMenuItem("Reindex").action {
					VitaOrganizerCache.deleteAll()
					updateFileList();
				})
			})
			add(JMenu(Texts.format("MENU_HELP")).apply {
				add(JMenuItem(Texts.format("MENU_WEBSITE"), Icons.WEBSITE).action {
					openWebpage(URL("http://github.com/soywiz/vitaorganizer"))
				})
				add(JSeparator())
				add(JMenuItem(Texts.format("MENU_CHECK_FOR_UPDATES"), Icons.DOWNLOAD).action {
					checkForUpdates()
				})
				add(JMenuItem(Texts.format("MENU_ABOUT"), Icons.ABOUT).action {
					openAbout()
				})
			})
		}

		//val columnNames = arrayOf("Icon", "ID", "Title")

		//val data = arrayOf(arrayOf(JLabel("Kathy"), "Smith", "Snowboarding", 5, false), arrayOf("John", "Doe", "Rowing", 3, true), arrayOf("Sue", "Black", "Knitting", 2, false), arrayOf("Jane", "White", "Speed reading", 20, true), arrayOf("Joe", "Brown", "Pool", 10, false))
		//table.rowSelectionAllowed = false
		//table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

		//Create the scroll pane and add the table to it.
		val scrollPane = table

		//Add the scroll pane to this panel.
		//val const = SpringLayout.Constraints()
		//const.setConstraint(SpringLayout.NORTH, Spring.constant(32, 32, 32))
		//const.height = Spring.constant(32, 32, 32)

		val footer = JPanel().apply {
			add(statusLabel)
		}

		val header = JPanel(FlowLayout(FlowLayout.LEFT)).apply {

			add(JButton(Texts.format("MENU_SELECT_FOLDER"), Icons.OPEN_FOLDER).action {
				selectFolder()
			})

			add(JButton(Texts.format("MENU_REFRESH"), Icons.REFRESH).action {
				updateFileList()
			})

			//val connectText = Texts.format("CONNECT_TO_PSVITA")
			var connected = false
			val connectAddress = object : JTextField(VitaOrganizerSettings.lastDeviceIp) {
				init {
					font = Font(Font.MONOSPACED, Font.PLAIN, 14)
					columns = 17;
				}

				override fun processKeyEvent(e: KeyEvent?) {
					super.processKeyEvent(e)
					VitaOrganizerSettings.lastDeviceIp = this.getText()
				}
			}.apply {
				addActionListener {
					println("aaa")
				}
			}

			//hotspotButton.icon

			//add(connectButton)
			add(connectAddress)
			add(JLabel(Texts.format("LABEL_FILTER")))
			add(filterTextField)

			if (OS.isWindows) {
				// http://superuser.com/questions/804227/how-to-get-assigned-ips-by-hostednetwork
				val hotspotButton = JButton("Hotspot")
				hotspotButton.icon = Icons.UNKNOWN
				fun updateWlanAsync() {
					hotspotQueue.queue {
						if (WifiNetwork.checkHostedNetwork()) {
							hotspotButton.icon = Icons.CONNECTED
							hotspotButton.text = "Hotspot: " + VitaOrganizerSettings.WLAN_SSID + " -- " + VitaOrganizerSettings.WLAN_PASS
						} else {
							hotspotButton.icon = Icons.DISCONNECTED
							hotspotButton.text = "Hotspot"
						}
					}
				}

				hotspotButton.onClick {
					hotspotButton.icon = Icons.UNKNOWN
					hotspotQueue.queue {
						if (WifiNetwork.checkHostedNetwork()) {
							WifiNetwork.stopHostedNetwork()
						} else {
							WifiNetwork.startHostedNetwork(VitaOrganizerSettings.WLAN_SSID, VitaOrganizerSettings.WLAN_PASS)
						}
						updateWlanAsync()
					}
				}

				updateWlanAsync()
				add(hotspotButton)
			}

			//filterTextField.requestFocus()
		}

		add(header, SpringLayout.NORTH)
		add(scrollPane)
		add(footer, SpringLayout.SOUTH)

		updateFileList()

		frame.addWindowListener(object : WindowAdapter() {
			override fun windowOpened(e: WindowEvent) {
				filterTextField.requestFocus()
			}
			override fun windowClosing(e: WindowEvent?) {
				if (runningTasks) {
					val confirmed = JOptionPane.showConfirmDialog(null, Texts.format("CONFIRM_EXIT_TEXT"), Texts.format("CONFIRM_EXIT_TITLE"), JOptionPane.YES_NO_OPTION)

					if (confirmed != JOptionPane.YES_OPTION) {
						frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
						return
					}
				}

				// If not cancelled
				VitaOrganizerSettings.ensureWriteSync()
				frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
				VitaOrganizerSettings.LANGUAGE_LOCALE
				frame.dispose()
				System.exit(0)
			}
		})

		frame.addComponentListener(object : ComponentAdapter() {
			override fun componentResized(e: ComponentEvent?) {
				super.componentResized(e)
				table.table.preferredScrollableViewportSize = Dimension(frame.width, frame.height)
				if (frame.getExtendedState() == JFrame.NORMAL) {
					VitaOrganizerSettings.WINDOW_WIDTH = frame.width
					VitaOrganizerSettings.WINDOW_HEIGHT = frame.height
					println("Updated size ${frame.width}x${frame.height}")
				} else{
					println("maximixed or minimized!")
				}
			}
		})

		//frame.focusOwner = filterTextField
	}

	fun openAbout() {
		frame.showDialog(AboutFrame())
	}

	fun checkForUpdates() {
		localTasks.queue(CheckForUpdatesTask(vitaOrganizer))
	}

	fun updateFileList() {
		localTasks.queue(UpdateFileListTask(vitaOrganizer))
	}

	//fun fileWatchFolder(path: String) {
	//	val watcher = FileSystems.getDefault().newWatchService()
	//	val dir = FileSystems.getDefault().getPath(path)
	//	try {
	//
	//		val key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
	//
	//	} catch (x: IOException) {
	//		System.err.println(x);
	//	}
	//}
}
