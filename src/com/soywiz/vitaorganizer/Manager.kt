package com.soywiz.vitaorganizer

import com.soywiz.util.OS
import com.soywiz.util.stream
import com.soywiz.vitaorganizer.ext.*
import it.sauronsoftware.ftp4j.FTPClient
import it.sauronsoftware.ftp4j.FTPDataTransferListener
import it.sauronsoftware.ftp4j.FTPException
import it.sauronsoftware.ftp4j.FTPFile
import it.sauronsoftware.ftp4j.FTPReply
import sun.nio.ch.FileKey
import java.io.*
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.*
import java.util.zip.*
import javax.swing.JOptionPane

/**
 * Created by super on 05.12.2016.
 */
object Manager {

}

object ConnectionMgr {
	var connected: Boolean = false
	var authenticated: Boolean = false
	var transferingdata: Boolean = false
	val ftp: FTPClient = FTPClient()
	val lock: Any = Any()

	init {
		ftp.setType(FTPClient.TYPE_BINARY)
		ftp.setPassive(true)
		setFtpTimeouts()
	}

	fun setFtpTimeouts() {
		ftp.connector.setCloseTimeout(20)
		ftp.connector.setReadTimeout(240) // PROM could take a lot of time!
		ftp.connector.setConnectionTimeout(120)
		ftp.connector.setCloseTimeout(5)
	}

	fun setConnectionStatus(connected: Boolean, authenticated: Boolean) {
		this.connected = connected
		this.authenticated = authenticated
	}

	fun isTransferingData(): Boolean {
		return transferingdata
	}

	fun setTransferingData(transfering: Boolean) {
		this.transferingdata = transfering
	}

	fun checkAddress(hostname: String, port: Int = 1337, timeout: Int = 200): Boolean {
		try {
			val sock = Socket()
			sock.connect(InetSocketAddress(hostname, port), timeout)
			sock.close()
			return true
		} catch (e: Throwable) {
			return false
		}
	}

	fun discoverIp(port: Int = 1337): List<String> {
		val ips = NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
		val ips2 = ips.map { it.hostAddress }.filter { it.startsWith("192.") }
		val ipEnd = Regex("\\.(\\d+)$")
		val availableIps = arrayListOf<String>()
		var rest = 256
		for (baseIp in ips2) {
			for (n in 0 until 256) {
				Thread {
					val ip = baseIp.replace(ipEnd, "\\.$n")
					if (checkAddress(ip, port)) {
						availableIps += ip
					}
					rest--
				}.start()
			}
		}
		while (rest > 0) {
			//println(rest)
			Thread.sleep(20L)
		}
		//println(availableIps)
		return availableIps
	}

	fun isConnected(): Boolean {
		//maybe send a NOOP to see if it throws an exception,
		//because ftp.isConnected()
		//is not reliable. The server could terminate the
		//connection without these functions knowing about it
		//immediatelly which causes us problems
		return ftp.isConnected() && this.connected;
	}

	fun isAuthenticated(): Boolean {
		return ftp.isAuthenticated() && this.authenticated;
	}

	fun isDisconnected() : Boolean {
		//again, not reliable
		return !ftp.isConnected() && !this.connected;
	}

	fun connectToFtp(hostname: String, port: Int = 1337, username: String = "", password: String = ""): Boolean {

		if(hostname.isEmpty())
			return false

		if(isConnected() && isAuthenticated()) {
			return true
		}

		try {
			println("Connecting to $hostname:$port...")
			val response: Array<String> = ftp.connect(hostname, port)
			val msg = response.get(0)
			println("Connected, welcome msg: $msg")
		}
		catch(e: IOException) {
			println("IOException: disconnecting")
			ftp.disconnect(false)
			setConnectionStatus(false, false)
			return false
		}
		catch(e: FTPException) {
			println("FTPException: disconnecting")
			ftp.disconnect(false)
			setConnectionStatus(false, false)
			return false
		}
		catch(e: IllegalStateException) {
			println("IllegalStateException: already connected")
		}

		if(!ftp.isConnected()) {
			println("Connection failed for unknown reason")
			ftp.abruptlyCloseCommunication()
			setConnectionStatus(false, false)
			return false
		}

		setConnectionStatus(true, false)

		try {
			println("Authenticating with username=\'$username\' and password=\'$password\'...")
			ftp.login(username, password)
		}
		catch(e: IOException) {
			println("IOException: I/O Error, disconnecting")
			ftp.disconnect(true)
			setConnectionStatus(false, false)
			return false
		}
		catch(e: FTPException) {
			println("FTPException: Login failed, disconnecting with QUIT message")
			ftp.disconnect(true)
			setConnectionStatus(false, false)
			return false
		}

		if(!ftp.isAuthenticated) {
			println("Login failed, disconnecting with QUIT message")
			ftp.disconnect(true)
			setConnectionStatus(false, false)
			return false
		}

		println("Authenticated")
		setConnectionStatus(true, true)
		ftp.setAutoNoopTimeout(30000)
		return true
	}

	fun disconnectFromFtp(): Boolean {
		try {
			if(this.isConnected()) {
				if(this.isAuthenticated()) {
					ftp.abortCurrentDataTransfer(true)
					ftp.setAutoNoopTimeout(0)
					ftp.logout()
					ftp.disconnect(true)
				}
				else {
					ftp.abortCurrentDataTransfer(false)
					ftp.disconnect(true)
				}
			}
		}
		catch(e: Throwable) {

		}
		finally {
			ftp.abortCurrentDataTransfer(false)
			ftp.disconnect(false);
			ftp.abruptlyCloseCommunication()
			setConnectionStatus(false, false)
			setTransferingData(false)
		}

		return isDisconnected()
	}

	fun getFtpClient(): FTPClient {
		return ftp
	}

	fun deleteFile(remotePath: String) : Boolean {
		if(remotePath.isEmpty())
			return false;

		val tmp = if(remotePath.startsWith('/')) "" else "/" + remotePath
		if(tmp.endsWith('/')) {
			//is this a directory?
			return false
		}

		if(!connectToFtp(VitaOrganizerSettings.lastDeviceIp, VitaOrganizerSettings.lastDevicePort))
			return false;

		try {
			getFtpClient().deleteFile(tmp)
		} catch (e: Throwable) {
			println("Can't delete file $tmp")
			return false;
		}
		return true;
	}

	fun downloadDirectory(remotePath: String, localPath: String) : Boolean {

		if(remotePath.isEmpty() || localPath.isEmpty())
			return false

		if(!connectToFtp(VitaOrganizerSettings.lastDeviceIp, VitaOrganizerSettings.lastDevicePort))
			return false

		var _remotePath = remotePath.replace("//", "/")
		var _localPath = localPath.replace("//", "/")

		val ftp = ConnectionMgr.getFtpClient();
		try {
			val localFile = File(localPath)
			if(!localFile.exists())
				localFile.mkdirs()

			for(file in ftp.list(remotePath)) {
				if(file.type == FTPFile.TYPE_DIRECTORY) {
					println("[D] $_remotePath/${file.name}/")
					val ret = downloadDirectory("$_remotePath/${file.name}", "$_localPath/${file.name}");
					if(!ret) {
						println("Could not download subdirectory $_remotePath${file.name}")
						return false
					}
				}
				else if(file.type == FTPFile.TYPE_FILE) {
					val f: File = File("$_localPath/${file.name}")
					if(!f.parentFile.safe_exists()) {
						val t: String = f.parent
						println("$t doesnt exsist!")
						return false
					}
					print("[F] $_remotePath/${file.name} -> ")
					ftp.download("$_remotePath/${file.name}", f, object : FTPDataTransferListener {
						override fun started() {
							print("started...")
						}

						override fun completed() {
							print("completed!")
						}

						override fun aborted() {
							print("aborted!")
						}
						override fun transferred(size: Int) {
							// print(".")
						}

						override fun failed() {
							print("failed!")
						}
					})
					println("")
				}
			}
		}
		catch(e: Throwable) {
			e.printStackTrace()
			return false
		}
		return true
	}
}

object IOMgr {
	fun exists(path: String): Boolean = File(path).safe_exists()
	fun isFile(path: String): Boolean = File(path).safe_isFile()
	fun isDirectory(path: String): Boolean = File(path).safe_isDirectory()
	fun canRead(path: String, recursive: Boolean = false): Boolean = File(path).safe_canRead()
	fun canWrite(path: String, recursive: Boolean = false): Boolean = File(path).safe_canWrite()
	fun canReadWrite(path: String): Boolean = File(path).safe_canReadWrite()
	fun canDelete(path: String, recursive: Boolean = false): Boolean = File(path).safe_canDelete()
	fun canExecute(path: String): Boolean = File(path).safe_canExecute()
	fun delete(path: String) : Boolean = File(path).safe_delete()
	fun createAndCheckFile(path: String) : Boolean = File(path).createAndCheckFile()
	fun listAllFiles(path: String) : MutableList<File> = File(path).listAllFiles()
}

object ZipMgr {

	private val multiplier = if(FileSize.base == 10) 1000 else 1024

	fun extractZip(zipFile: File, directory: File) : Boolean {

		if(!directory.safe_isDirectory())
			return false

		val dirPath = directory.canonicalPath

		val fis = FileInputStream(zipFile)
		try {
			val zis = ZipInputStream(fis)
			var ze = zis.nextEntry

			while(ze != null) {
				val outputFile = File(dirPath + File.separator + ze.name)
				println("Unzipping ${ze.name} to ${outputFile.canonicalPath}")

				if(ze.name.endsWith('/'))
					outputFile.mkdirs()
				else {
					outputFile.parentFile.mkdirs()
					val fos = FileOutputStream(outputFile)

					val bytes = ByteArray(10 * multiplier * multiplier)

					while (true) {
						val length = zis.read(bytes)
						if (length < 0)
							break;

						fos.write(bytes, 0, length)
					}

					fos.close()
				}
				ze = zis.nextEntry
			}

			zis.close()
		}
		catch(e: Throwable) {
			e.printStackTrace()
			fis.close();
			return false;
		}

		return true
	}

	fun writeZipFile(dir: File, extension: String = ".zip"): Boolean {
		if(!dir.safe_isDirectory())
			return false

		val fileList = dir.listAllFiles()
		if(fileList.isEmpty())
			return false

		try {
			val fos = FileOutputStream("${dir.parent}/${dir.name}$extension")
			val zos = ZipOutputStream(fos)
			zos.setMethod(java.util.zip.Deflater.DEFLATED)
			zos.setLevel(9)
			for (file in fileList) {
				if(!addFileFromDirectory(file, dir, zos)) {
					println("failed $file!")
					return false
				}
			}

			zos.close()
			fos.close()
			return true
		}
		catch (e: Throwable) {
			e.printStackTrace()
			return false
		}
	}

	fun addFileFromDirectory(file: File, dir: File, zos: ZipOutputStream): Boolean {
		if(!file.safe_canRead()) {
			println("cannot read file")
			return false
		}
		if(!file.safe_isDirectory()) {
			println("given directory isnt one")
			return false
		}

		try {
			var filePath = file.getCanonicalPath().substring(dir.getCanonicalPath().length + 1,
				file.getCanonicalPath().length)
			filePath = filePath.replace("\\", "/")
			println("Writing $filePath to zip")

			//val deflated = DeflaterInputStream(fis)
			if(file.isDirectory) {
				zos.putNextEntry(ZipEntry(filePath + if(!filePath.endsWith('/')) "/" else ""))
				zos.closeEntry()
				return true
			}

			zos.putNextEntry(ZipEntry(filePath))

			val fis = FileInputStream(file)
			val bytes = ByteArray(10 * multiplier * multiplier)

			while(true) {
				val length = fis.read(bytes)
				if(length < 0)
					break;

				zos.write(bytes, 0, length)
			}

			zos.closeEntry();
			//deflated.close()
			fis.close()

			return true
		}
		catch(e: Throwable) {
			println("exception ZipMgr::addFileFromDirectory")
			e.printStackTrace()
			return false;
		}
	}
}

object MsgMgr {
	fun info(message: String, title: String = Texts.format("INFORMATION")) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE)
	}

	fun error(message: String, title: String = "Error") {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE)
	}

	fun warn(message: String, title: String): Boolean {
		val result = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION)
		return (result == JOptionPane.YES_OPTION)
	}
}

object UsbStorageMgr {
	  fun findUsbStorage() : String? {
		  if(OS.isWindows) {

		  }
		  else if(OS.isMac) {

		  }
		  else if(OS.isUnix) {

		  }
		  else if(OS.isSolaris) {

		  }
		  return null
	  }

	fun isMemoryCard(dirPathFile: File) : Boolean {

		if(!dirPathFile.safe_exists() || !dirPathFile.safe_isDirectory() || !dirPathFile.safe_canReadWrite())
			return false

		val content = dirPathFile.list()
		val isValid = content.contains("app") && content.contains("data") && content.contains(
			"user")

		return isValid
	}

	fun hasVitaShell(dirPathFile: File) : Boolean {
		//if(!isMemoryCard(dirPathFile))
		//	return false
		val paramSfoPath = dirPathFile.canonicalPath + File.separator + "app" + File.separator + "VITASHELL" + File.separator + "sce_sys" + File.separator + "param.sfo"
		val paramSfoFile = File(paramSfoPath)
		if(!paramSfoFile.safe_exists() || !paramSfoFile.safe_canRead())
			return false;

		val psf = PSF.read(paramSfoFile.readBytes().stream)
		val APP_VER = psf["APP_VER"].toString().toDouble()

		if(APP_VER < 1.51f) {
			MsgMgr.error("You must have at least VitaShell 01.51!")
			return false
		}

		return true
	}
}

//TODO: for future -> support anyrhing we can thru one class, that collects all available data to the pointed entry at location
class PSP2Entry(location: String) {
	init {
		if(location.isEmpty()) {
			throw Exception("PSP2Entry::init --> location is empty")
		}
		//figure out what contains
		val file = File(location)
		if(file.safe_exists()) {
			if(file.safe_isFile()) {
				if(file.extension.toLowerCase() == "vpk") {
					//normal installable vpk files
				}
				else if(file.extension.toLowerCase() == "zip") {
					//maybe also a vpk, maybe a maidunp, maidump_patch, maidump_addc
				}
			}
			else if(file.safe_isDirectory()) {
				//extraced vpk folder; maidump, maidump_patch, maidump_addc folder
			}
		}
		else if(location.startsWith("ux0:app/")) {
			//on memory card (usb or ftp)
		}
		else if(location.startsWith("ur0:app/")) {
			//or internal flash memory (ftp)
		}
		else if(location.startsWith("http://") || location.startsWith("https://")) {
			//support downloading a game
		}
	}
}
