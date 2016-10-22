package com.soywiz.vitaorganizer

import com.soywiz.util.OS
import com.soywiz.util.open2
import com.soywiz.vitaorganizer.ext.*
import com.soywiz.vitaorganizer.popups.AboutFrame
import com.soywiz.vitaorganizer.popups.KeyValueViewerFrame
import com.soywiz.vitaorganizer.tasks.*
import java.awt.*
import java.awt.event.*
import java.io.File
import java.net.URL
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class VitaOrganizer : JPanel(BorderLayout()), StatusUpdater {
	companion object {
		@JvmStatic fun main(args: Array<String>) {
			VitaOrganizerSettings.init()
			VitaOrganizerSettings.WLAN_SSID
			VitaOrganizerSettings.WLAN_PASS
			VitaOrganizer().start()
		}
	}

	val vitaOrganizer = this@VitaOrganizer
	val localTasks = VitaTaskQueue(this)
	val remoteTasks = VitaTaskQueue(this)
	val hotspotQueue = VitaTaskQueue(this)

	init {
		Texts.setLanguage(VitaOrganizerSettings.LANGUAGE_LOCALE)
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
	}

	val frame = JFrame("VitaOrganizer ${VitaOrganizerVersion.currentVersion}").apply {
		defaultCloseOperation = JFrame.EXIT_ON_CLOSE
		iconImage = ImageIO.read(getResourceURL("com/soywiz/vitaorganizer/icon.png"))
	}

	fun start() {
		frame.pack()
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

	val VPK_GAME_IDS = hashSetOf<String>()
	val VITA_GAME_IDS = hashSetOf<String>()

	val statusLabel = JLabel(Texts.format("STEP_STARTED"))

	override fun updateStatus(status: String) {
		//println(status)
		SwingUtilities.invokeLater {
			statusLabel.text = status
		}
	}

	fun updateEntries() {
		val ALL_GAME_IDS = LinkedHashMap<String, CachedGameEntry>()

		fun getGameEntryById(gameId: String) = ALL_GAME_IDS.getOrPut(gameId) { CachedGameEntry(gameId) }

		synchronized(VPK_GAME_IDS) {
			for (gameId in VPK_GAME_IDS) getGameEntryById(gameId).inPC = true
		}
		synchronized(VITA_GAME_IDS) {
			for (gameId in VITA_GAME_IDS) getGameEntryById(gameId).inVita = true
		}

		table.setEntries(ALL_GAME_IDS.values.toList())
	}

	fun showFileInExplorerOrFinder(file: File) {
		if(!file.exists())
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
			this.isEnabled = false
		}
		val gameDumperVersionPopup = JMenuItem("").apply {
			this.isEnabled = false
		}
		val gameCompressionLevelPopup = JMenuItem("").apply {
			this.isEnabled = false
		}

		val popupMenu = object : JPopupMenu() {
			var entry: CachedGameEntry? = null

			val deleteFromVita = JMenuItem(Texts.format("DELETE_FROM_PSVITA_ACTION")).action {
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
				//this.isEnabled = false
			}

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
				if (entry != null) {
					if(entry!!.inPC || (entry!!.inPC && entry!!.inVita)) {
						showFileInExplorerOrFinder(entry!!.vpkLocalFile!!)
					}
				}
			}

			val repackVpk = JMenuItem(Texts.format("MENU_REPACK")).action {
				if (entry != null) remoteTasks.queue(RepackVpkTask(vitaOrganizer, entry!!, setSecure = true))
			}

			val showPSF = JMenuItem(Texts.format("MENU_SHOW_PSF")).action {
				if (entry != null) {
					frame.showDialog(KeyValueViewerFrame(Texts.format("PSF_VIEWER_TITLE", "id" to entry!!.id, "title" to entry!!.title), entry!!.psf))
				}
			}

			init {
				add(gameTitlePopup)
				add(JSeparator())
				add(gameDumperVersionPopup)
				add(gameCompressionLevelPopup)
				add(JSeparator())
				add(showInFilebrowser)
				add(showPSF)
				add(repackVpk)

				add(JSeparator())
				add(JMenuItem(Texts.format("METHOD1_INFO")).apply {
					isEnabled = false
				})
				add(sendVpkToVita)
				add(sendDataToVita)
				add(JSeparator())
				add(JMenuItem(Texts.format("METHOD2_INFO")).apply {
					isEnabled = false
				})
				add(sendToVita1Step)
			}

			override fun show(invoker: Component?, x: Int, y: Int) {
				val entry = entry
				gameTitlePopup.text = Texts.format("UNKNOWN_VERSION")
				gameDumperVersionPopup.text = Texts.format("UNKNOWN_VERSION")
				gameCompressionLevelPopup.text = Texts.format("UNKNOWN_VERSION")
				deleteFromVita.isEnabled = false
				sendVpkToVita.isEnabled = false
				sendDataToVita.isEnabled = false
				sendToVita1Step.isEnabled = false
				showInFilebrowser.isEnabled = false
				repackVpk.isEnabled = false
				showPSF.isEnabled = false;

				if (entry != null) {
					gameDumperVersionPopup.text = Texts.format("DUMPER_VERSION", "version" to entry.dumperVersion)
					gameCompressionLevelPopup.text = Texts.format("COMPRESSION_LEVEL", "level" to entry.compressionLevel)
					gameTitlePopup.text = "${entry.id} : ${entry.title}"
					gameTitlePopup.setForeground(Color(64, 0,255));
					gameTitlePopup.setFont(gameTitlePopup.getFont().deriveFont(Font.BOLD));
					deleteFromVita.isEnabled = entry.inVita
					sendVpkToVita.isEnabled = entry.inPC
					sendDataToVita.isEnabled = entry.inPC
					sendToVita1Step.isEnabled = entry.inPC
					showInFilebrowser.isEnabled = entry.inPC
					repackVpk.isEnabled = entry.inPC
					showPSF.isEnabled = true
				}

				super.show(invoker, x, y)
			}
		}

		override fun showMenuAtFor(x: Int, y: Int, entry: CachedGameEntry) {
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

	fun setLanguageText(name: String) {
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
				add(JMenuItem(Texts.format("MENU_INSTALL_VPK")).action {
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
				add(JMenuItem(Texts.format("MENU_SELECT_FOLDER")).action {
					selectFolder()
				})
				add(JMenuItem(Texts.format("MENU_REFRESH")).action {
					updateFileList()
				})
				add(JSeparator())
				add(JMenuItem(Texts.format("MENU_EXIT")).action {
					System.exit(0)
				})
			})
			add(JMenu(Texts.format("MENU_SETTINGS")).apply {
				add(JMenu(Texts.format("MENU_LANGUAGES")).apply {
					add(JRadioButtonMenuItem(Texts.format("MENU_LANGUAGE_AUTODETECT")).apply {
						this.isSelected = VitaOrganizerSettings.isLanguageAutodetect
					}.action {
						setLanguageText("auto")
					})
					add(JSeparator())
					for (l in Texts.SUPPORTED_LOCALES) {
						val lrb = JRadioButtonMenuItem(l.getDisplayLanguage(l).capitalize()).apply {
							this.isSelected = VitaOrganizerSettings.LANGUAGE_LOCALE == l
						}
						//languageList[l.language] = lrb
						add(lrb).action {
							setLanguageText(l.language)
						}
					}
					Unit
				})
			})
			add(JMenu(Texts.format("MENU_HELP")).apply {
				add(JMenuItem(Texts.format("MENU_WEBSITE")).action {
					openWebpage(URL("http://github.com/soywiz/vitaorganizer"))
				})
				add(JSeparator())
				add(JMenuItem(Texts.format("MENU_CHECK_FOR_UPDATES")).action {
					checkForUpdates()
				})
				add(JMenuItem(Texts.format("MENU_ABOUT")).action {
					openAbout()
				})
			})
		}

		//val columnNames = arrayOf("Icon", "ID", "Title")

		//val data = arrayOf(arrayOf(JLabel("Kathy"), "Smith", "Snowboarding", 5, false), arrayOf("John", "Doe", "Rowing", 3, true), arrayOf("Sue", "Black", "Knitting", 2, false), arrayOf("Jane", "White", "Speed reading", 20, true), arrayOf("Joe", "Brown", "Pool", 10, false))


		table.table.preferredScrollableViewportSize = Dimension(960, 600)
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

			add(JButton(Texts.format("MENU_SELECT_FOLDER")).action {
				selectFolder()
			})

			add(JButton(Texts.format("MENU_REFRESH")).action {
				updateFileList()
			})

			val connectText = Texts.format("CONNECT_TO_PSVITA")
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


			val connectButton = object : JButton(connectText) {
				val button = this

				fun disconnect() {
                    if(PsvitaDevice.disconnectFromFtp()) {
					    connected = false
					    button.text = connectText
					    synchronized(VITA_GAME_IDS) {
						    VITA_GAME_IDS.clear()
					    }
					    updateEntries()
					    updateStatus(Texts.format("DISCONNECTED"))
                    }
                    else
                        println("Failed to disconnect!")
				}

				fun connect(ip: String) {
					connected = true
					VitaOrganizerSettings.lastDeviceIp = ip
					button.text = Texts.format("DISCONNECT_FROM_IP", "ip" to ip)
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
								updateStatus(Texts.format("PROCESSING_GAME", "current" to (vitaGameCount + 1), "total" to vitaGameIds.size, "gameId" to gameId, "id" to gameId))
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

						updateStatus(Texts.format("CONNECTED"))
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

                        updateEntries()
					}.start()
				}

				init {
					val button = this
                    
                    addActionListener al@ {e:ActionEvent ->
					    if (connected) {
						    this@VitaOrganizer.updateStatus(Texts.format("DISCONNECTING"))
						    disconnect()
					    } else {
                            var ip = connectAddress.getText()
                                
                            if(ip == "") {
                                println("No ip given")
                                JOptionPane.showMessageDialog(frame, "Please type in an ip address!")
                                return@al
                            }
                            
                            if(!PsvitaDevice.checkAddress(ip)) {
                                println("No connection could be etablished!");
                                JOptionPane.showMessageDialog(frame, "No connection could be etablished!\nCheck your ip or start the FTP server!")
                                return@al
                            }
                                
						    this@VitaOrganizer.updateStatus(Texts.format("CONNECTING"))
						    button.button.isEnabled = false

						    connect(ip)
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
				}
			}

			//hotspotButton.icon

			add(connectButton)
			add(connectAddress)
			add(JLabel(Texts.format("LABEL_FILTER")))
			add(filterTextField)

			if (OS.isWindows) {
				// http://superuser.com/questions/804227/how-to-get-assigned-ips-by-hostednetwork
				val hotspotButton = JButton("Hotspot")
				hotspotButton.icon = ConnectionIcons.UNKNOWN
				fun updateWlanAsync() {
					hotspotQueue.queue {
						if (WifiNetwork.checkHostedNetwork()) {
							hotspotButton.icon = ConnectionIcons.CONNECTED
							hotspotButton.text = "Hotspot: " + VitaOrganizerSettings.WLAN_SSID + " -- " + VitaOrganizerSettings.WLAN_PASS
						} else {
							hotspotButton.icon = ConnectionIcons.DISCONNECTED
							hotspotButton.text = "Hotspot"
						}
					}
				}

				hotspotButton.onClick {
					hotspotButton.icon = ConnectionIcons.UNKNOWN
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
		})

		//frame.focusOwner = filterTextField
	}

	object ConnectionIcons {
		val UNKNOWN = ImageIcon(VitaOrganizer::class.java.classLoader.getResource("com/soywiz/vitaorganizer/icons/icon_unknown.png"))
		val CONNECTED = ImageIcon(VitaOrganizer::class.java.classLoader.getResource("com/soywiz/vitaorganizer/icons/icon_connected.png"))
		val DISCONNECTED = ImageIcon(VitaOrganizer::class.java.classLoader.getResource("com/soywiz/vitaorganizer/icons/icon_disconnected.png"))
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
