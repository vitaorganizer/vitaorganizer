package com.soywiz.vitaorganizer

import it.sauronsoftware.ftp4j.FTPClient
import it.sauronsoftware.ftp4j.FTPDataTransferListener
import it.sauronsoftware.ftp4j.FTPException
import it.sauronsoftware.ftp4j.FTPFile
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.*
import java.util.zip.ZipFile

object PsvitaDevice {
    fun checkAddress(ip: String, port: Int = 1337): Boolean {
        try {
            val sock = Socket()
            sock.connect(InetSocketAddress(ip, port), 200)
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

    val ftp = FTPClient().apply {
        type = FTPClient.TYPE_BINARY
        //autoNoopTimeout = 50000L
        this.connector.setCloseTimeout(20)
        this.connector.setReadTimeout(240) // PROM could take a lot of time!
        this.connector.setConnectionTimeout(120)
    }

    //init {
        //ftp.sendCustomCommand()
    //}

    var ip = "192.168.1.130"
    var port = 1337

    fun setIp(ip: String, port: Int = 1337) {
        this.ip = ip
        this.port = port
    }

    private fun connectedFtp(): FTPClient {
        retries@for (n in 0 until 5) {
            if (!ftp.isConnected) {
                ftp.connect(ip, port)
                ftp.login("", "")
            }
            try {
                ftp.noop()
                break@retries
            } catch (e: IOException) {
                ftp.disconnect(false)
            }
        }
        return ftp
    }

    fun getGameIds() = connectedFtp().list("/ux0:/app").filter { i -> i.type == it.sauronsoftware.ftp4j.FTPFile.TYPE_DIRECTORY }.map { File(it.name).name }

    fun getGameFolder(id: String) = "/ux0:/app/${File(id).name}"

    fun downloadSmallFile(path: String): ByteArray {
        val file = File.createTempFile("vita", "download")
        try {
            connectedFtp().download(path, file)
            return file.readBytes()
        } catch (e: FTPException) {

        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            file.delete()
        }
        return byteArrayOf()
    }

    fun getParamSfo(id: String): ByteArray = downloadSmallFile("${getGameFolder(id)}/sce_sys/param.sfo")
    fun getGameIcon(id: String): ByteArray = downloadSmallFile("${getGameFolder(id)}/sce_sys/icon0.png")
    fun downloadEbootBin(id: String): ByteArray = downloadSmallFile("${getGameFolder(id)}/eboot.bin")

    fun getParamSfoCached(id: String): ByteArray {
        val file = VitaOrganizerCache.entry(id).paramSfoFile
        if (!file.exists()) file.writeBytes(getParamSfo(id))
        return file.readBytes()
    }

    fun getGameIconCached(id: String): ByteArray {
        val file = VitaOrganizerCache.entry(id).icon0File
        if (!file.exists()) file.writeBytes(getGameIcon(id))
        return file.readBytes()
    }


    fun getFolderSize(path: String, folderSizeCache: HashMap<String, Long> = hashMapOf<String, Long>()): Long {
        return folderSizeCache.getOrPut(path) {
            var out = 0L
            val ftp = connectedFtp()
            try {
                for (file in ftp.list(path)) {
                    if (file.type == FTPFile.TYPE_DIRECTORY) {
                        getFolderSize("$path/${file.name}", folderSizeCache)
                    } else {
                        out += file.size
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            out
        }
    }

    fun getGameSize(id: String, folderSizeCache: HashMap<String, Long> = hashMapOf<String, Long>()): Long {
        return getFolderSize(getGameFolder(id), folderSizeCache)
    }

    class Status() {
        var currentFile: Int = 0
        var totalFiles: Int = 0
        var currentSize: Long = 0L
        var totalSize: Long = 0L

        val fileRange: String get() = "$currentFile/$totalFiles"
        val sizeRange: String get() = "${FileSize.toString(currentSize)}/${FileSize.toString(totalSize)}"
    }

    val createDirectoryCache = hashSetOf<String>()

    fun createDirectories(path: String, createDirectoryCache: HashSet<String> = PsvitaDevice.createDirectoryCache) {
        val parent = File(path).parent
        if (parent != "" && parent != null) {
            createDirectories(parent, createDirectoryCache)
        }
        if (path !in createDirectoryCache) {
            println("Creating directory $path...")
            createDirectoryCache.add(path)
            try {
                connectedFtp().createDirectory(path)
            } catch (e: IOException) {
                throw e
            } catch (e: FTPException) {
                e.printStackTrace()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun uploadGame(id: String, zip: ZipFile, filter: (path: String) -> Boolean = { true }, updateStatus: (Status) -> Unit = { }) {
        val base = getGameFolder(id)

        val status = Status()

        val unfilteredEntries = zip.entries().toList()

        val filteredEntries = unfilteredEntries.filter { filter(it.name) }

        status.currentFile = 0
        status.totalFiles = filteredEntries.size

        status.currentSize = 0L
        status.totalSize = filteredEntries.map { it.size }.sum()

        for (entry in filteredEntries) {
            val vname = "$base/${entry.name}"
            val directory = File(vname).parent
            val startSize = status.currentSize
            println("Writting $vname...")
            if (!entry.isDirectory) {
                createDirectories(directory)
                connectedFtp().upload(vname, zip.getInputStream(entry), 0L, 0L, object : FTPDataTransferListener {
                    override fun started() {
                    }

                    override fun completed() {
                        updateStatus(status)
                    }

                    override fun aborted() {
                    }

                    override fun transferred(size: Int) {
                        status.currentSize += size
                        updateStatus(status)
                    }

                    override fun failed() {
                    }
                })
            }
            status.currentSize = startSize + entry.size
            status.currentFile++
            updateStatus(status)
        }

        println("DONE. Now package should be promoted!")
    }

    fun uploadFile(path: String, data: ByteArray, updateStatus: (Status) -> Unit = { }) {
        val status = Status()
        createDirectories(File(path).parent)
        status.currentFile = 0
        status.totalFiles = 1
        status.totalSize = data.size.toLong()
        updateStatus(status)
        connectedFtp().upload(path, ByteArrayInputStream(data), 0L, 0L, object : FTPDataTransferListener {
            override fun started() {
            }

            override fun completed() {
            }

            override fun aborted() {
            }

            override fun transferred(size: Int) {
                status.currentSize += size
                updateStatus(status)
            }

            override fun failed() {
            }
        })
        status.currentFile++
        updateStatus(status)
    }

    fun removeFile(path: String) {
        connectedFtp().deleteFile(path)
    }

    fun promoteVpk(vpkPath: String) {
        connectedFtp().sendCustomCommand("PROM $vpkPath")
    }

    /*
    fun writePackage() {
        val PACKAGE_PARENT = "ux0:ptmp"
        val PACKAGE_DIR = "$PACKAGE_PARENT/pkg"
        val HEAD_BIN = "$PACKAGE_DIR/sce_sys/package/head.bin"
        // promote(PACKAGE_DIR)
    }
    */

    /*

void loadScePaf() {
	uint32_t ptr[0x100] = { 0 };
	ptr[0] = 0;
	ptr[1] = (uint32_t)&ptr[0];
	uint32_t scepaf_argp[] = { 0x400000, 0xEA60, 0x40000, 0, 0 };
	sceSysmoduleLoadModuleInternalWithArg(0x80000008, sizeof(scepaf_argp), scepaf_argp, ptr);
}

int promote(char *path) {
	int res;

	loadScePaf();

	res = sceSysmoduleLoadModuleInternal(SCE_SYSMODULE_PROMOTER_UTIL);
	res = scePromoterUtilityInit();
	res = scePromoterUtilityPromotePkg(path, 0);

	int state = 0;
	do {
		res = scePromoterUtilityGetState(&state);
		sceKernelDelayThread(300 * 1000);
	} while (state);

	int result = 0;
	res = scePromoterUtilityGetResult(&result);
	res = scePromoterUtilityExit();
	res = sceSysmoduleUnloadModuleInternal(SCE_SYSMODULE_PROMOTER_UTIL);
	return result;
}


	// Make head.bin
	res = makeHeadBin();
	if (res < 0) {
		closeWaitDialog();
		errorDialog(res);
		goto EXIT;
	}

	// Promote
	res = promote(PACKAGE_DIR);
	if (res < 0) {
		closeWaitDialog();
		errorDialog(res);
		goto EXIT;
	}

    int makeHeadBin() {
        uint8_t hmac[16];
        uint32_t off;
        uint32_t len;
        uint32_t out;

        // head.bin must not be present
        SceIoStat stat;
        memset(&stat, 0, sizeof(SceIoStat));
        if (sceIoGetstat(HEAD_BIN, &stat) >= 0)
            return -1;

        // Get title id
        char *title_id = get_title_id(PACKAGE_DIR "/sce_sys/param.sfo");
        if (!title_id)// || strlen(title_id) != 9) // Enforce TITLEID format?
            return -2;

        // Allocate head.bin buffer
        uint8_t *head_bin = malloc(sizeof(base_head_bin));
        memcpy(head_bin, base_head_bin, sizeof(base_head_bin));

        // Write full titleid
        char full_title_id[128];
        snprintf(full_title_id, sizeof(full_title_id), "EP9000-%s_00-XXXXXXXXXXXXXXXX", title_id);
        strncpy((char *)&head_bin[0x30], full_title_id, 48);

        // hmac of pkg header
        len = ntohl(*(uint32_t *)&head_bin[0xD0]);
        fpkg_hmac(&head_bin[0], len, hmac);
        memcpy(&head_bin[len], hmac, 16);

        // hmac of pkg info
        off = ntohl(*(uint32_t *)&head_bin[0x8]);
        len = ntohl(*(uint32_t *)&head_bin[0x10]);
        out = ntohl(*(uint32_t *)&head_bin[0xD4]);
        fpkg_hmac(&head_bin[off], len - 64, hmac);
        memcpy(&head_bin[out], hmac, 16);

        // hmac of everything
        len = ntohl(*(uint32_t *)&head_bin[0xE8]);
        fpkg_hmac(&head_bin[0], len, hmac);
        memcpy(&head_bin[len], hmac, 16);

        // Make dir
        sceIoMkdir(PACKAGE_DIR "/sce_sys/package", 0777);

        // Write head.bin
        WriteFile(HEAD_BIN, head_bin, sizeof(base_head_bin));

        free(head_bin);
        free(title_id);

        return 0;
    }

     */
}